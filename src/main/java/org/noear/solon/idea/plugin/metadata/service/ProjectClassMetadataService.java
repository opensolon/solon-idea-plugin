package org.noear.solon.idea.plugin.metadata.service;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiTreeChangeAdapter;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PropertyUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.noear.solon.idea.plugin.metadata.index.AggregatedMetadataIndex;
import org.noear.solon.idea.plugin.metadata.index.ConfigurationMetadataIndex;
import org.noear.solon.idea.plugin.metadata.index.MetadataIndex;
import org.noear.solon.idea.plugin.metadata.index.MetadataProperty;
import org.noear.solon.idea.plugin.metadata.source.ConfigurationMetadata;
import org.noear.solon.idea.plugin.metadata.source.PropertyName;
import org.noear.solon.idea.plugin.misc.PsiTypeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.function.Predicate.not;
import static org.noear.solon.idea.plugin.misc.PsiTypeUtils.getCanonicalTextOfType;

/**
 * Service that provides {@link MetadataIndex} from {@link MetadataProperty#getFullType()}.
 * <p>
 * And watch for update continuously.
 */
@Service(Service.Level.PROJECT)
public final class ProjectClassMetadataService implements Disposable {
  private static final Logger LOG = Logger.getInstance(ProjectClassMetadataService.class);

  /** 防止自引用类型（如 {@code Node parent}）导致无限递归。 */
  private static final int MAX_NESTED_DEPTH = 5;

  private final Project project;


  public ProjectClassMetadataService(Project project) {
    this.project = project;
    PsiManager.getInstance(project).addPsiTreeChangeListener(new PsiTreeChangeAdapter() {
      //TODO Watch Psi changes
      //TODO Watch class's modification & update metadata
    }, this);
  }


  @NotNull
  public Optional<MetadataIndex> getMetadata(@NotNull String baseName, @NotNull PsiType type) {
    if (PsiTypeUtils.isValueType(type)) {
      return Optional.empty();
    }
    return Optional.of(generateMetadataInSmartMode(new AggregatedMetadataIndex(), PropertyName.of(baseName), type, 0))
        .filter(not(MetadataIndex::isEmpty));
  }


  /**
   * 为单值绑定生成一条配置项元数据，例如
   * {@code @Inject("${track.name}") String trackUrl}。
   * <p>单值类型（String、int、enum 等）无法从类结构展开，必须显式登记，
   * 否则该 key 既不能从配置反向跳转，也无法从字段正向跳转。</p>
   */
  @NotNull
  public Optional<MetadataIndex> getPropertyMetadata(
      @NotNull String name, @NotNull PsiType type, @NotNull PsiElement sourceElement) {
    ConfigurationMetadata metadata = new ConfigurationMetadata();
    ConfigurationMetadata.Property meta = new ConfigurationMetadata.Property();
    meta.setName(name);
    meta.setType(getCanonicalTextOfType(type));
    PsiClass containingClass = sourceElement instanceof PsiMember member
        ? member.getContainingClass()
        // 方法参数不是 PsiMember，向上找所在类，保证至少能跳转到声明处
        : PsiTreeUtil.getParentOfType(sourceElement, PsiClass.class);
    if (containingClass != null) {
      meta.setSourceType(containingClass.getQualifiedName());
    }
    metadata.getProperties().add(meta);
    return Optional.of(new ConfigurationMetadataIndex(metadata, sourceElement, project));
  }


  @NotNull
  private MetadataIndex generateMetadataInSmartMode(
      AggregatedMetadataIndex index, PropertyName basename, PsiType type, int depth
  ) {
    return DumbService.getInstance(project)
        .runReadActionInSmartMode(() -> generateMetadata(index, basename, type, depth));
  }


  @NotNull
  private MetadataIndex generateMetadata(
      AggregatedMetadataIndex index, PropertyName basename, PsiType type, int depth) {
    LOG.debug("Generating metadata for: " + basename + " -> " + type.getPresentableText());
    if (depth > MAX_NESTED_DEPTH) {
      return index;
    }
    if (PsiTypeUtils.isValueType(type)) {
      // Exit condition: value type do not need to index.
      return index;
    }
    if (PsiTypeUtils.isMap(project, type)) {
      try {
        PsiType[] kvType = PsiTypeUtils.getKeyValueType(project, type);
        if (!(kvType != null && kvType.length == 2)) {
          LOG.warn("Unsupported map type: " + type);
          return index;
        }
        if (!PsiTypeUtils.isValueType(kvType[0])) {
          LOG.warn(basename + " has unsupported Map key type: " + type);
          return index;
        }
        generateMetadata(index, basename.appendAnyMapKey(), kvType[1], depth + 1);
      } catch (Exception e) {
        LOG.warn(basename + " has illegal Map type: " + type);
        return index;
      }
    } else if (PsiTypeUtils.isCollection(project, type)) {
      try {
        PsiType elementType = PsiTypeUtils.getElementType(project, type);
        assert elementType != null;
        generateMetadata(index, basename.appendAnyNumericalIndex(), elementType, depth + 1);
      } catch (Exception e) {
        LOG.warn(basename + " has illegal Collection type: " + type);
        return index;
      }
    } else {
      PsiClass valueClass = PsiTypeUtils.resolveClassInType(type);
      if (valueClass == null) return index;
      String sourceTypeName = valueClass.getQualifiedName();
      if (sourceTypeName == null) return index;
      if (valueClass.isInterface() || valueClass.isAnnotationType() || valueClass.isEnum()) {
        return index;
      }

      ConfigurationMetadata metadata = new ConfigurationMetadata();
      // MetadataIndexBase#add 未对 null 做保护，这里必须初始化为空集合
      metadata.setProperties(new ArrayList<>());
      metadata.setGroups(new ArrayList<>());

      // 登记为 group：让 key（如 ctsz.request 或 ctsz.request.oauth2）本身可跳转到配置类
      ConfigurationMetadata.Group group = new ConfigurationMetadata.Group();
      group.setName(basename.toString());
      group.setType(sourceTypeName);
      group.setSourceType(sourceTypeName);
      metadata.getGroups().add(group);

      for (PsiField field : collectConfigFields(valueClass)) {
        // 保持字段原始驼峰名，与 Solon 官方 metadata（如 server.wrapPort）的约定一致；
        // 驼峰 / 中划线 / 大小写的等价性由 ConfigurationPropertyName 的宽松比较负责。
        PropertyName name = basename.append(field.getName());
        PsiType propertyType = field.getType();
        if (propertyType instanceof PsiPrimitiveType primitiveType) {
          propertyType = primitiveType.getBoxedType(field);
        }
        if (propertyType == null) continue;

        // 每个字段都登记为属性：保证 yaml key ↔ 字段 双向可达，
        // 包括 List/Map/嵌套对象字段（否则只能跳到类、无法落到字段）。
        ConfigurationMetadata.Property meta = new ConfigurationMetadata.Property();
        meta.setName(name.toString());
        meta.setType(getCanonicalTextOfType(propertyType));
        meta.setSourceType(sourceTypeName);
        PsiExpression initializer = field.getInitializer();
        if (initializer instanceof PsiLiteralExpression literal) {
          meta.setDefaultValue(literal.getValue());
        }
        metadata.getProperties().add(meta);

        if (!PsiTypeUtils.isValueType(propertyType)) {
          // 嵌套对象 / 容器元素继续展开
          generateMetadata(index, name, propertyType, depth + 1);
        }
      }
      index.addLast(new ConfigurationMetadataIndex(metadata, valueClass, project));
    }
    return index;
  }


  /**
   * 收集配置类的可绑定字段。
   *
   * <p>优先使用 JavaBean 语义的可写属性（getter/setter）；若类上没有真实 setter
   * （典型场景：Lombok {@code @Data} 且 IDE 未安装 Lombok 插件），
   * 则回退到直接枚举实例字段，保证配置项不会丢失。</p>
   */
  @NotNull
  private static List<PsiField> collectConfigFields(@NotNull PsiClass valueClass) {
    Map<String, PsiField> fields = new LinkedHashMap<>();
    for (String propertyName : PropertyUtil.getWritableProperties(valueClass, true)) {
      PsiField field = valueClass.findFieldByName(propertyName, true);
      if (field != null && !field.hasModifierProperty(PsiModifier.STATIC)) {
        fields.putIfAbsent(propertyName, field);
      }
    }
    for (PsiField field : valueClass.getAllFields()) {
      if (field.hasModifierProperty(PsiModifier.STATIC)) continue;
      if (field instanceof PsiEnumConstant) continue;
      fields.putIfAbsent(field.getName(), field);
    }
    return new ArrayList<>(fields.values());
  }


  @Override
  public void dispose() {
  }
}
