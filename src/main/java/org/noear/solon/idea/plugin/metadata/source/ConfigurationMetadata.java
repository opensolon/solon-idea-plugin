package org.noear.solon.idea.plugin.metadata.source;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents all entries present in {@code classpath:/META-INF/solon-configuration-metadata.json}.
 */
@Data
public class ConfigurationMetadata {
  /**
   * The "groups" are higher level items that do not themselves specify a value but instead provide a contextual grouping for properties.
   */
  @Nullable
  private List<Group> groups;
  /**
   * Each "property" is a configuration item that the user specifies with a given value.
   */
  private List<Property> properties = new ArrayList<>();
  /**
   * "hints" are additional information used to assist the user in configuring a given property.
   */
  @Nullable
  private List<Hint> hints;


  public boolean isEmpty() {
    return (hints == null || hints.isEmpty()) &&
        (groups == null || groups.isEmpty()) &&
        (properties == null || properties.isEmpty());
  }


  /**
   * The "groups" are higher level items that do not themselves specify a value but instead provide a contextual grouping for properties.
   */
  @Data
  public static class Group {
    /**
     * The full name of the group. This attribute is mandatory.
     */
    private String name;
    /**
     * The class name of the data type of the group. For example, if the group were based on a class annotated with
     * {@code @ConfigurationProperties}, the attribute would contain the fully qualified name of that class.
     * If it were based on a @Bean method, it would be the return type of that method.
     * If the type is not known, the attribute may be omitted.
     */
    @Nullable
    private String type;
    /**
     * A short description of the group that can be displayed to users. If no description is available, it may be omitted.
     * It is recommended that descriptions be short paragraphs, with the first line providing a concise summary.
     * The last line in the description should end with a period (.).
     */
    @Nullable
    private String description;
    /**
     * The class name of the source that contributed this group. For example, if the group were based on a {@code @Bean} method
     * annotated with {@code @ConfigurationProperties}, this attribute would contain the fully qualified name of the
     * {@code @Configuration} class that contains the method. If the source type is not known, the attribute may be omitted.
     */
    @Nullable
    private String sourceType;
    /**
     * The full name of the method (include parenthesis and argument types) that contributed this group
     * (for example, the name of a {@code @ConfigurationProperties} annotated {@code @Bean} method).
     * If the source method is not known, it may be omitted.
     */
    @Nullable
    private String sourceMethod;
  }


  /**
   * Each "property" is a configuration item that the user specifies with a given value.
   */
  @Data
  public static class Property {
    private String name;
    @Nullable
    private String type;
    @Nullable
    private String description;
    @Nullable
    private String sourceType;
    @Nullable
    private Object defaultValue;
    @Nullable
    private Deprecation deprecation;


    @Data
    public static class Deprecation {
      /**
       * The level of deprecation, which can be either warning (the default) or error.
       * When a property has a warning deprecation level, it should still be bound in the environment.
       * However, when it has an error deprecation level, the property is no longer managed and is not bound.
       */
      @Nullable
      private Level level = Level.WARNING;
      /**
       * A short description of the reason why the property was deprecated.
       * If no reason is available, it may be omitted.
       * It is recommended that descriptions be short paragraphs, with the first line providing a concise summary.
       * The last line in the description should end with a period (.).
       */
      @Nullable
      private String reason;
      /**
       * The full name of the property that replaces this deprecated property.
       * If there is no replacement for this property, it may be omitted.
       */
      @Nullable
      private String replacement;


      /**
       * The level of deprecation, which can be either warning or error.
       * When a property has a warning deprecation level, it should still be bound in the environment.
       * However, when it has an error deprecation level, the property is no longer managed and is not bound.
       */
      public enum Level {
        /**
         * When a property has a warning deprecation level, it should still be bound in the environment.
         */
        @SerializedName("warning")
        WARNING,
        /**
         * However, when it has an error deprecation level, the property is no longer managed and is not bound.
         */
        @SerializedName("error")
        ERROR
      }
    }
  }


  /**
   * "hints" are additional information used to assist the user in configuring a given property.
   */
  @Data
  public static class Hint {

    private String name;
    /**
     * A list of valid values as defined by the ValueHint object.
     * Each entry defines the value and may have a description.
     */
    @Nullable
    private ValueHint[] values;
    /**
     * A list of providers as defined by the ValueProvider object.
     * Each entry defines the name of the provider and its parameters, if any.
     */
    @Nullable
    private ValueProvider[] providers;


    @Data
    public static class ValueHint {
      /**
       * A valid value for the element to which the hint refers.
       * If the type of the property is an array, it can also be an array of value(s).
       * This attribute is mandatory.
       */
      private Object value;
      /**
       * A short description of the value that can be displayed to users.
       * If no description is available, it may be omitted.
       * It is recommended that descriptions be short paragraphs, with the first line providing a concise summary.
       * The last line in the description should end with a period (.).
       */
      @Nullable
      private String description;
    }


    @Data
    public static class ValueProvider {
      /**
       * The name of the provider to use to offer additional content assistance for the element to which the hint refers.
       */
      private Type name;
      /**
       * Any additional parameter that the provider supports (check the documentation of the provider for more details).
       */
      @Nullable
      private Map<String, Object> parameters;


      public enum Type {
        @SerializedName("any")
        ANY,
        @SerializedName("class-reference")
        CLASS_REFERENCE,
        @SerializedName("handle-as")
        HANDLE_AS,
        @SerializedName("logger-name")
        LOGGER_NAME
      }
    }
  }
}
