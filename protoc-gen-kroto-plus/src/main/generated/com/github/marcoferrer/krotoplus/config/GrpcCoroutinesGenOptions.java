// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: krotoplus/compiler/config.proto

package com.github.marcoferrer.krotoplus.config;

/**
 * <pre>
 * Configuration used by the 'gRPC Coroutines' code generator.
 * </pre>
 *
 * Protobuf type {@code krotoplus.compiler.GrpcCoroutinesGenOptions}
 */
public  final class GrpcCoroutinesGenOptions extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:krotoplus.compiler.GrpcCoroutinesGenOptions)
    GrpcCoroutinesGenOptionsOrBuilder {
private static final long serialVersionUID = 0L;
  // Use GrpcCoroutinesGenOptions.newBuilder() to construct.
  private GrpcCoroutinesGenOptions(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private GrpcCoroutinesGenOptions() {
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new GrpcCoroutinesGenOptions();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private GrpcCoroutinesGenOptions(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
    com.google.protobuf.UnknownFieldSet.Builder unknownFields =
        com.google.protobuf.UnknownFieldSet.newBuilder();
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          case 10: {
            com.github.marcoferrer.krotoplus.config.FileFilter.Builder subBuilder = null;
            if (filter_ != null) {
              subBuilder = filter_.toBuilder();
            }
            filter_ = input.readMessage(com.github.marcoferrer.krotoplus.config.FileFilter.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(filter_);
              filter_ = subBuilder.buildPartial();
            }

            break;
          }
          default: {
            if (!parseUnknownField(
                input, unknownFields, extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
    } finally {
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return com.github.marcoferrer.krotoplus.config.Config.internal_static_krotoplus_compiler_GrpcCoroutinesGenOptions_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.github.marcoferrer.krotoplus.config.Config.internal_static_krotoplus_compiler_GrpcCoroutinesGenOptions_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions.class, com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions.Builder.class);
  }

  public static final int FILTER_FIELD_NUMBER = 1;
  private com.github.marcoferrer.krotoplus.config.FileFilter filter_;
  /**
   * <pre>
   * Filter used for limiting the input files that are processed by the code generator
   * The default filter will match true against all input files.
   * </pre>
   *
   * <code>.krotoplus.compiler.FileFilter filter = 1;</code>
   */
  public boolean hasFilter() {
    return filter_ != null;
  }
  /**
   * <pre>
   * Filter used for limiting the input files that are processed by the code generator
   * The default filter will match true against all input files.
   * </pre>
   *
   * <code>.krotoplus.compiler.FileFilter filter = 1;</code>
   */
  public com.github.marcoferrer.krotoplus.config.FileFilter getFilter() {
    return filter_ == null ? com.github.marcoferrer.krotoplus.config.FileFilter.getDefaultInstance() : filter_;
  }
  /**
   * <pre>
   * Filter used for limiting the input files that are processed by the code generator
   * The default filter will match true against all input files.
   * </pre>
   *
   * <code>.krotoplus.compiler.FileFilter filter = 1;</code>
   */
  public com.github.marcoferrer.krotoplus.config.FileFilterOrBuilder getFilterOrBuilder() {
    return getFilter();
  }

  private byte memoizedIsInitialized = -1;
  @java.lang.Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @java.lang.Override
  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (filter_ != null) {
      output.writeMessage(1, getFilter());
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (filter_ != null) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(1, getFilter());
    }
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions)) {
      return super.equals(obj);
    }
    com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions other = (com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions) obj;

    if (hasFilter() != other.hasFilter()) return false;
    if (hasFilter()) {
      if (!getFilter()
          .equals(other.getFilter())) return false;
    }
    if (!unknownFields.equals(other.unknownFields)) return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    if (hasFilter()) {
      hash = (37 * hash) + FILTER_FIELD_NUMBER;
      hash = (53 * hash) + getFilter().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * <pre>
   * Configuration used by the 'gRPC Coroutines' code generator.
   * </pre>
   *
   * Protobuf type {@code krotoplus.compiler.GrpcCoroutinesGenOptions}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:krotoplus.compiler.GrpcCoroutinesGenOptions)
      com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptionsOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.github.marcoferrer.krotoplus.config.Config.internal_static_krotoplus_compiler_GrpcCoroutinesGenOptions_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.github.marcoferrer.krotoplus.config.Config.internal_static_krotoplus_compiler_GrpcCoroutinesGenOptions_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions.class, com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions.Builder.class);
    }

    // Construct using com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3
              .alwaysUseFieldBuilders) {
      }
    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      if (filterBuilder_ == null) {
        filter_ = null;
      } else {
        filter_ = null;
        filterBuilder_ = null;
      }
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return com.github.marcoferrer.krotoplus.config.Config.internal_static_krotoplus_compiler_GrpcCoroutinesGenOptions_descriptor;
    }

    @java.lang.Override
    public com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions getDefaultInstanceForType() {
      return com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions.getDefaultInstance();
    }

    @java.lang.Override
    public com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions build() {
      com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions buildPartial() {
      com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions result = new com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions(this);
      if (filterBuilder_ == null) {
        result.filter_ = filter_;
      } else {
        result.filter_ = filterBuilder_.build();
      }
      onBuilt();
      return result;
    }

    @java.lang.Override
    public Builder clone() {
      return super.clone();
    }
    @java.lang.Override
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.setField(field, value);
    }
    @java.lang.Override
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }
    @java.lang.Override
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }
    @java.lang.Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
      return super.setRepeatedField(field, index, value);
    }
    @java.lang.Override
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.addRepeatedField(field, value);
    }
    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions) {
        return mergeFrom((com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions other) {
      if (other == com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions.getDefaultInstance()) return this;
      if (other.hasFilter()) {
        mergeFilter(other.getFilter());
      }
      this.mergeUnknownFields(other.unknownFields);
      onChanged();
      return this;
    }

    @java.lang.Override
    public final boolean isInitialized() {
      return true;
    }

    @java.lang.Override
    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private com.github.marcoferrer.krotoplus.config.FileFilter filter_;
    private com.google.protobuf.SingleFieldBuilderV3<
        com.github.marcoferrer.krotoplus.config.FileFilter, com.github.marcoferrer.krotoplus.config.FileFilter.Builder, com.github.marcoferrer.krotoplus.config.FileFilterOrBuilder> filterBuilder_;
    /**
     * <pre>
     * Filter used for limiting the input files that are processed by the code generator
     * The default filter will match true against all input files.
     * </pre>
     *
     * <code>.krotoplus.compiler.FileFilter filter = 1;</code>
     */
    public boolean hasFilter() {
      return filterBuilder_ != null || filter_ != null;
    }
    /**
     * <pre>
     * Filter used for limiting the input files that are processed by the code generator
     * The default filter will match true against all input files.
     * </pre>
     *
     * <code>.krotoplus.compiler.FileFilter filter = 1;</code>
     */
    public com.github.marcoferrer.krotoplus.config.FileFilter getFilter() {
      if (filterBuilder_ == null) {
        return filter_ == null ? com.github.marcoferrer.krotoplus.config.FileFilter.getDefaultInstance() : filter_;
      } else {
        return filterBuilder_.getMessage();
      }
    }
    /**
     * <pre>
     * Filter used for limiting the input files that are processed by the code generator
     * The default filter will match true against all input files.
     * </pre>
     *
     * <code>.krotoplus.compiler.FileFilter filter = 1;</code>
     */
    public Builder setFilter(com.github.marcoferrer.krotoplus.config.FileFilter value) {
      if (filterBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        filter_ = value;
        onChanged();
      } else {
        filterBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <pre>
     * Filter used for limiting the input files that are processed by the code generator
     * The default filter will match true against all input files.
     * </pre>
     *
     * <code>.krotoplus.compiler.FileFilter filter = 1;</code>
     */
    public Builder setFilter(
        com.github.marcoferrer.krotoplus.config.FileFilter.Builder builderForValue) {
      if (filterBuilder_ == null) {
        filter_ = builderForValue.build();
        onChanged();
      } else {
        filterBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <pre>
     * Filter used for limiting the input files that are processed by the code generator
     * The default filter will match true against all input files.
     * </pre>
     *
     * <code>.krotoplus.compiler.FileFilter filter = 1;</code>
     */
    public Builder mergeFilter(com.github.marcoferrer.krotoplus.config.FileFilter value) {
      if (filterBuilder_ == null) {
        if (filter_ != null) {
          filter_ =
            com.github.marcoferrer.krotoplus.config.FileFilter.newBuilder(filter_).mergeFrom(value).buildPartial();
        } else {
          filter_ = value;
        }
        onChanged();
      } else {
        filterBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <pre>
     * Filter used for limiting the input files that are processed by the code generator
     * The default filter will match true against all input files.
     * </pre>
     *
     * <code>.krotoplus.compiler.FileFilter filter = 1;</code>
     */
    public Builder clearFilter() {
      if (filterBuilder_ == null) {
        filter_ = null;
        onChanged();
      } else {
        filter_ = null;
        filterBuilder_ = null;
      }

      return this;
    }
    /**
     * <pre>
     * Filter used for limiting the input files that are processed by the code generator
     * The default filter will match true against all input files.
     * </pre>
     *
     * <code>.krotoplus.compiler.FileFilter filter = 1;</code>
     */
    public com.github.marcoferrer.krotoplus.config.FileFilter.Builder getFilterBuilder() {
      
      onChanged();
      return getFilterFieldBuilder().getBuilder();
    }
    /**
     * <pre>
     * Filter used for limiting the input files that are processed by the code generator
     * The default filter will match true against all input files.
     * </pre>
     *
     * <code>.krotoplus.compiler.FileFilter filter = 1;</code>
     */
    public com.github.marcoferrer.krotoplus.config.FileFilterOrBuilder getFilterOrBuilder() {
      if (filterBuilder_ != null) {
        return filterBuilder_.getMessageOrBuilder();
      } else {
        return filter_ == null ?
            com.github.marcoferrer.krotoplus.config.FileFilter.getDefaultInstance() : filter_;
      }
    }
    /**
     * <pre>
     * Filter used for limiting the input files that are processed by the code generator
     * The default filter will match true against all input files.
     * </pre>
     *
     * <code>.krotoplus.compiler.FileFilter filter = 1;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
        com.github.marcoferrer.krotoplus.config.FileFilter, com.github.marcoferrer.krotoplus.config.FileFilter.Builder, com.github.marcoferrer.krotoplus.config.FileFilterOrBuilder> 
        getFilterFieldBuilder() {
      if (filterBuilder_ == null) {
        filterBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
            com.github.marcoferrer.krotoplus.config.FileFilter, com.github.marcoferrer.krotoplus.config.FileFilter.Builder, com.github.marcoferrer.krotoplus.config.FileFilterOrBuilder>(
                getFilter(),
                getParentForChildren(),
                isClean());
        filter_ = null;
      }
      return filterBuilder_;
    }
    @java.lang.Override
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:krotoplus.compiler.GrpcCoroutinesGenOptions)
  }

  // @@protoc_insertion_point(class_scope:krotoplus.compiler.GrpcCoroutinesGenOptions)
  private static final com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions();
  }

  public static com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<GrpcCoroutinesGenOptions>
      PARSER = new com.google.protobuf.AbstractParser<GrpcCoroutinesGenOptions>() {
    @java.lang.Override
    public GrpcCoroutinesGenOptions parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new GrpcCoroutinesGenOptions(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<GrpcCoroutinesGenOptions> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<GrpcCoroutinesGenOptions> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

