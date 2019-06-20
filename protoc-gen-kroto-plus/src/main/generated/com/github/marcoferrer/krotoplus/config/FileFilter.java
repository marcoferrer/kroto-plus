// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: krotoplus/compiler/config.proto

package com.github.marcoferrer.krotoplus.config;

/**
 * <pre>
 * Represent a filter used for including and excluding source files from
 * being processed by a code generator. It is inclusive by default, so
 * all paths compared against its default instance will be included as
 * input to a generator and processed.
 * </pre>
 *
 * Protobuf type {@code krotoplus.compiler.FileFilter}
 */
public  final class FileFilter extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:krotoplus.compiler.FileFilter)
    FileFilterOrBuilder {
private static final long serialVersionUID = 0L;
  // Use FileFilter.newBuilder() to construct.
  private FileFilter(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private FileFilter() {
    includePath_ = com.google.protobuf.LazyStringArrayList.EMPTY;
    excludePath_ = com.google.protobuf.LazyStringArrayList.EMPTY;
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private FileFilter(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
    int mutable_bitField0_ = 0;
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
            java.lang.String s = input.readStringRequireUtf8();
            if (!((mutable_bitField0_ & 0x00000001) != 0)) {
              includePath_ = new com.google.protobuf.LazyStringArrayList();
              mutable_bitField0_ |= 0x00000001;
            }
            includePath_.add(s);
            break;
          }
          case 18: {
            java.lang.String s = input.readStringRequireUtf8();
            if (!((mutable_bitField0_ & 0x00000002) != 0)) {
              excludePath_ = new com.google.protobuf.LazyStringArrayList();
              mutable_bitField0_ |= 0x00000002;
            }
            excludePath_.add(s);
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
      if (((mutable_bitField0_ & 0x00000001) != 0)) {
        includePath_ = includePath_.getUnmodifiableView();
      }
      if (((mutable_bitField0_ & 0x00000002) != 0)) {
        excludePath_ = excludePath_.getUnmodifiableView();
      }
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return com.github.marcoferrer.krotoplus.config.Config.internal_static_krotoplus_compiler_FileFilter_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.github.marcoferrer.krotoplus.config.Config.internal_static_krotoplus_compiler_FileFilter_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.github.marcoferrer.krotoplus.config.FileFilter.class, com.github.marcoferrer.krotoplus.config.FileFilter.Builder.class);
  }

  public static final int INCLUDE_PATH_FIELD_NUMBER = 1;
  private com.google.protobuf.LazyStringList includePath_;
  /**
   * <pre>
   * List of file paths to include as inputs for a code generator.
   * A valid value starts from the root package directory of the source file. Globs are supported
   * ie.
   * krotoplus/compiler/config.proto
   * krotoplus/&#42;*
   * **&#47;compiler/con*.proto
   * </pre>
   *
   * <code>repeated string include_path = 1;</code>
   */
  public com.google.protobuf.ProtocolStringList
      getIncludePathList() {
    return includePath_;
  }
  /**
   * <pre>
   * List of file paths to include as inputs for a code generator.
   * A valid value starts from the root package directory of the source file. Globs are supported
   * ie.
   * krotoplus/compiler/config.proto
   * krotoplus/&#42;*
   * **&#47;compiler/con*.proto
   * </pre>
   *
   * <code>repeated string include_path = 1;</code>
   */
  public int getIncludePathCount() {
    return includePath_.size();
  }
  /**
   * <pre>
   * List of file paths to include as inputs for a code generator.
   * A valid value starts from the root package directory of the source file. Globs are supported
   * ie.
   * krotoplus/compiler/config.proto
   * krotoplus/&#42;*
   * **&#47;compiler/con*.proto
   * </pre>
   *
   * <code>repeated string include_path = 1;</code>
   */
  public java.lang.String getIncludePath(int index) {
    return includePath_.get(index);
  }
  /**
   * <pre>
   * List of file paths to include as inputs for a code generator.
   * A valid value starts from the root package directory of the source file. Globs are supported
   * ie.
   * krotoplus/compiler/config.proto
   * krotoplus/&#42;*
   * **&#47;compiler/con*.proto
   * </pre>
   *
   * <code>repeated string include_path = 1;</code>
   */
  public com.google.protobuf.ByteString
      getIncludePathBytes(int index) {
    return includePath_.getByteString(index);
  }

  public static final int EXCLUDE_PATH_FIELD_NUMBER = 2;
  private com.google.protobuf.LazyStringList excludePath_;
  /**
   * <pre>
   * List of file paths to exclude as inputs for a code generator.
   * a valid value start from the root package directory of the source file. Globs are supported
   * ie. google/&#42;
   * </pre>
   *
   * <code>repeated string exclude_path = 2;</code>
   */
  public com.google.protobuf.ProtocolStringList
      getExcludePathList() {
    return excludePath_;
  }
  /**
   * <pre>
   * List of file paths to exclude as inputs for a code generator.
   * a valid value start from the root package directory of the source file. Globs are supported
   * ie. google/&#42;
   * </pre>
   *
   * <code>repeated string exclude_path = 2;</code>
   */
  public int getExcludePathCount() {
    return excludePath_.size();
  }
  /**
   * <pre>
   * List of file paths to exclude as inputs for a code generator.
   * a valid value start from the root package directory of the source file. Globs are supported
   * ie. google/&#42;
   * </pre>
   *
   * <code>repeated string exclude_path = 2;</code>
   */
  public java.lang.String getExcludePath(int index) {
    return excludePath_.get(index);
  }
  /**
   * <pre>
   * List of file paths to exclude as inputs for a code generator.
   * a valid value start from the root package directory of the source file. Globs are supported
   * ie. google/&#42;
   * </pre>
   *
   * <code>repeated string exclude_path = 2;</code>
   */
  public com.google.protobuf.ByteString
      getExcludePathBytes(int index) {
    return excludePath_.getByteString(index);
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
    for (int i = 0; i < includePath_.size(); i++) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, includePath_.getRaw(i));
    }
    for (int i = 0; i < excludePath_.size(); i++) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, excludePath_.getRaw(i));
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    {
      int dataSize = 0;
      for (int i = 0; i < includePath_.size(); i++) {
        dataSize += computeStringSizeNoTag(includePath_.getRaw(i));
      }
      size += dataSize;
      size += 1 * getIncludePathList().size();
    }
    {
      int dataSize = 0;
      for (int i = 0; i < excludePath_.size(); i++) {
        dataSize += computeStringSizeNoTag(excludePath_.getRaw(i));
      }
      size += dataSize;
      size += 1 * getExcludePathList().size();
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
    if (!(obj instanceof com.github.marcoferrer.krotoplus.config.FileFilter)) {
      return super.equals(obj);
    }
    com.github.marcoferrer.krotoplus.config.FileFilter other = (com.github.marcoferrer.krotoplus.config.FileFilter) obj;

    if (!getIncludePathList()
        .equals(other.getIncludePathList())) return false;
    if (!getExcludePathList()
        .equals(other.getExcludePathList())) return false;
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
    if (getIncludePathCount() > 0) {
      hash = (37 * hash) + INCLUDE_PATH_FIELD_NUMBER;
      hash = (53 * hash) + getIncludePathList().hashCode();
    }
    if (getExcludePathCount() > 0) {
      hash = (37 * hash) + EXCLUDE_PATH_FIELD_NUMBER;
      hash = (53 * hash) + getExcludePathList().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static com.github.marcoferrer.krotoplus.config.FileFilter parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.github.marcoferrer.krotoplus.config.FileFilter parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.github.marcoferrer.krotoplus.config.FileFilter parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.github.marcoferrer.krotoplus.config.FileFilter parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.github.marcoferrer.krotoplus.config.FileFilter parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.github.marcoferrer.krotoplus.config.FileFilter parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.github.marcoferrer.krotoplus.config.FileFilter parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.github.marcoferrer.krotoplus.config.FileFilter parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.github.marcoferrer.krotoplus.config.FileFilter parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static com.github.marcoferrer.krotoplus.config.FileFilter parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.github.marcoferrer.krotoplus.config.FileFilter parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.github.marcoferrer.krotoplus.config.FileFilter parseFrom(
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
  public static Builder newBuilder(com.github.marcoferrer.krotoplus.config.FileFilter prototype) {
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
   * Represent a filter used for including and excluding source files from
   * being processed by a code generator. It is inclusive by default, so
   * all paths compared against its default instance will be included as
   * input to a generator and processed.
   * </pre>
   *
   * Protobuf type {@code krotoplus.compiler.FileFilter}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:krotoplus.compiler.FileFilter)
      com.github.marcoferrer.krotoplus.config.FileFilterOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.github.marcoferrer.krotoplus.config.Config.internal_static_krotoplus_compiler_FileFilter_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.github.marcoferrer.krotoplus.config.Config.internal_static_krotoplus_compiler_FileFilter_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.github.marcoferrer.krotoplus.config.FileFilter.class, com.github.marcoferrer.krotoplus.config.FileFilter.Builder.class);
    }

    // Construct using com.github.marcoferrer.krotoplus.config.FileFilter.newBuilder()
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
      includePath_ = com.google.protobuf.LazyStringArrayList.EMPTY;
      bitField0_ = (bitField0_ & ~0x00000001);
      excludePath_ = com.google.protobuf.LazyStringArrayList.EMPTY;
      bitField0_ = (bitField0_ & ~0x00000002);
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return com.github.marcoferrer.krotoplus.config.Config.internal_static_krotoplus_compiler_FileFilter_descriptor;
    }

    @java.lang.Override
    public com.github.marcoferrer.krotoplus.config.FileFilter getDefaultInstanceForType() {
      return com.github.marcoferrer.krotoplus.config.FileFilter.getDefaultInstance();
    }

    @java.lang.Override
    public com.github.marcoferrer.krotoplus.config.FileFilter build() {
      com.github.marcoferrer.krotoplus.config.FileFilter result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public com.github.marcoferrer.krotoplus.config.FileFilter buildPartial() {
      com.github.marcoferrer.krotoplus.config.FileFilter result = new com.github.marcoferrer.krotoplus.config.FileFilter(this);
      int from_bitField0_ = bitField0_;
      if (((bitField0_ & 0x00000001) != 0)) {
        includePath_ = includePath_.getUnmodifiableView();
        bitField0_ = (bitField0_ & ~0x00000001);
      }
      result.includePath_ = includePath_;
      if (((bitField0_ & 0x00000002) != 0)) {
        excludePath_ = excludePath_.getUnmodifiableView();
        bitField0_ = (bitField0_ & ~0x00000002);
      }
      result.excludePath_ = excludePath_;
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
      if (other instanceof com.github.marcoferrer.krotoplus.config.FileFilter) {
        return mergeFrom((com.github.marcoferrer.krotoplus.config.FileFilter)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.github.marcoferrer.krotoplus.config.FileFilter other) {
      if (other == com.github.marcoferrer.krotoplus.config.FileFilter.getDefaultInstance()) return this;
      if (!other.includePath_.isEmpty()) {
        if (includePath_.isEmpty()) {
          includePath_ = other.includePath_;
          bitField0_ = (bitField0_ & ~0x00000001);
        } else {
          ensureIncludePathIsMutable();
          includePath_.addAll(other.includePath_);
        }
        onChanged();
      }
      if (!other.excludePath_.isEmpty()) {
        if (excludePath_.isEmpty()) {
          excludePath_ = other.excludePath_;
          bitField0_ = (bitField0_ & ~0x00000002);
        } else {
          ensureExcludePathIsMutable();
          excludePath_.addAll(other.excludePath_);
        }
        onChanged();
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
      com.github.marcoferrer.krotoplus.config.FileFilter parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (com.github.marcoferrer.krotoplus.config.FileFilter) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }
    private int bitField0_;

    private com.google.protobuf.LazyStringList includePath_ = com.google.protobuf.LazyStringArrayList.EMPTY;
    private void ensureIncludePathIsMutable() {
      if (!((bitField0_ & 0x00000001) != 0)) {
        includePath_ = new com.google.protobuf.LazyStringArrayList(includePath_);
        bitField0_ |= 0x00000001;
       }
    }
    /**
     * <pre>
     * List of file paths to include as inputs for a code generator.
     * A valid value starts from the root package directory of the source file. Globs are supported
     * ie.
     * krotoplus/compiler/config.proto
     * krotoplus/&#42;*
     * **&#47;compiler/con*.proto
     * </pre>
     *
     * <code>repeated string include_path = 1;</code>
     */
    public com.google.protobuf.ProtocolStringList
        getIncludePathList() {
      return includePath_.getUnmodifiableView();
    }
    /**
     * <pre>
     * List of file paths to include as inputs for a code generator.
     * A valid value starts from the root package directory of the source file. Globs are supported
     * ie.
     * krotoplus/compiler/config.proto
     * krotoplus/&#42;*
     * **&#47;compiler/con*.proto
     * </pre>
     *
     * <code>repeated string include_path = 1;</code>
     */
    public int getIncludePathCount() {
      return includePath_.size();
    }
    /**
     * <pre>
     * List of file paths to include as inputs for a code generator.
     * A valid value starts from the root package directory of the source file. Globs are supported
     * ie.
     * krotoplus/compiler/config.proto
     * krotoplus/&#42;*
     * **&#47;compiler/con*.proto
     * </pre>
     *
     * <code>repeated string include_path = 1;</code>
     */
    public java.lang.String getIncludePath(int index) {
      return includePath_.get(index);
    }
    /**
     * <pre>
     * List of file paths to include as inputs for a code generator.
     * A valid value starts from the root package directory of the source file. Globs are supported
     * ie.
     * krotoplus/compiler/config.proto
     * krotoplus/&#42;*
     * **&#47;compiler/con*.proto
     * </pre>
     *
     * <code>repeated string include_path = 1;</code>
     */
    public com.google.protobuf.ByteString
        getIncludePathBytes(int index) {
      return includePath_.getByteString(index);
    }
    /**
     * <pre>
     * List of file paths to include as inputs for a code generator.
     * A valid value starts from the root package directory of the source file. Globs are supported
     * ie.
     * krotoplus/compiler/config.proto
     * krotoplus/&#42;*
     * **&#47;compiler/con*.proto
     * </pre>
     *
     * <code>repeated string include_path = 1;</code>
     */
    public Builder setIncludePath(
        int index, java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  ensureIncludePathIsMutable();
      includePath_.set(index, value);
      onChanged();
      return this;
    }
    /**
     * <pre>
     * List of file paths to include as inputs for a code generator.
     * A valid value starts from the root package directory of the source file. Globs are supported
     * ie.
     * krotoplus/compiler/config.proto
     * krotoplus/&#42;*
     * **&#47;compiler/con*.proto
     * </pre>
     *
     * <code>repeated string include_path = 1;</code>
     */
    public Builder addIncludePath(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  ensureIncludePathIsMutable();
      includePath_.add(value);
      onChanged();
      return this;
    }
    /**
     * <pre>
     * List of file paths to include as inputs for a code generator.
     * A valid value starts from the root package directory of the source file. Globs are supported
     * ie.
     * krotoplus/compiler/config.proto
     * krotoplus/&#42;*
     * **&#47;compiler/con*.proto
     * </pre>
     *
     * <code>repeated string include_path = 1;</code>
     */
    public Builder addAllIncludePath(
        java.lang.Iterable<java.lang.String> values) {
      ensureIncludePathIsMutable();
      com.google.protobuf.AbstractMessageLite.Builder.addAll(
          values, includePath_);
      onChanged();
      return this;
    }
    /**
     * <pre>
     * List of file paths to include as inputs for a code generator.
     * A valid value starts from the root package directory of the source file. Globs are supported
     * ie.
     * krotoplus/compiler/config.proto
     * krotoplus/&#42;*
     * **&#47;compiler/con*.proto
     * </pre>
     *
     * <code>repeated string include_path = 1;</code>
     */
    public Builder clearIncludePath() {
      includePath_ = com.google.protobuf.LazyStringArrayList.EMPTY;
      bitField0_ = (bitField0_ & ~0x00000001);
      onChanged();
      return this;
    }
    /**
     * <pre>
     * List of file paths to include as inputs for a code generator.
     * A valid value starts from the root package directory of the source file. Globs are supported
     * ie.
     * krotoplus/compiler/config.proto
     * krotoplus/&#42;*
     * **&#47;compiler/con*.proto
     * </pre>
     *
     * <code>repeated string include_path = 1;</code>
     */
    public Builder addIncludePathBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      ensureIncludePathIsMutable();
      includePath_.add(value);
      onChanged();
      return this;
    }

    private com.google.protobuf.LazyStringList excludePath_ = com.google.protobuf.LazyStringArrayList.EMPTY;
    private void ensureExcludePathIsMutable() {
      if (!((bitField0_ & 0x00000002) != 0)) {
        excludePath_ = new com.google.protobuf.LazyStringArrayList(excludePath_);
        bitField0_ |= 0x00000002;
       }
    }
    /**
     * <pre>
     * List of file paths to exclude as inputs for a code generator.
     * a valid value start from the root package directory of the source file. Globs are supported
     * ie. google/&#42;
     * </pre>
     *
     * <code>repeated string exclude_path = 2;</code>
     */
    public com.google.protobuf.ProtocolStringList
        getExcludePathList() {
      return excludePath_.getUnmodifiableView();
    }
    /**
     * <pre>
     * List of file paths to exclude as inputs for a code generator.
     * a valid value start from the root package directory of the source file. Globs are supported
     * ie. google/&#42;
     * </pre>
     *
     * <code>repeated string exclude_path = 2;</code>
     */
    public int getExcludePathCount() {
      return excludePath_.size();
    }
    /**
     * <pre>
     * List of file paths to exclude as inputs for a code generator.
     * a valid value start from the root package directory of the source file. Globs are supported
     * ie. google/&#42;
     * </pre>
     *
     * <code>repeated string exclude_path = 2;</code>
     */
    public java.lang.String getExcludePath(int index) {
      return excludePath_.get(index);
    }
    /**
     * <pre>
     * List of file paths to exclude as inputs for a code generator.
     * a valid value start from the root package directory of the source file. Globs are supported
     * ie. google/&#42;
     * </pre>
     *
     * <code>repeated string exclude_path = 2;</code>
     */
    public com.google.protobuf.ByteString
        getExcludePathBytes(int index) {
      return excludePath_.getByteString(index);
    }
    /**
     * <pre>
     * List of file paths to exclude as inputs for a code generator.
     * a valid value start from the root package directory of the source file. Globs are supported
     * ie. google/&#42;
     * </pre>
     *
     * <code>repeated string exclude_path = 2;</code>
     */
    public Builder setExcludePath(
        int index, java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  ensureExcludePathIsMutable();
      excludePath_.set(index, value);
      onChanged();
      return this;
    }
    /**
     * <pre>
     * List of file paths to exclude as inputs for a code generator.
     * a valid value start from the root package directory of the source file. Globs are supported
     * ie. google/&#42;
     * </pre>
     *
     * <code>repeated string exclude_path = 2;</code>
     */
    public Builder addExcludePath(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  ensureExcludePathIsMutable();
      excludePath_.add(value);
      onChanged();
      return this;
    }
    /**
     * <pre>
     * List of file paths to exclude as inputs for a code generator.
     * a valid value start from the root package directory of the source file. Globs are supported
     * ie. google/&#42;
     * </pre>
     *
     * <code>repeated string exclude_path = 2;</code>
     */
    public Builder addAllExcludePath(
        java.lang.Iterable<java.lang.String> values) {
      ensureExcludePathIsMutable();
      com.google.protobuf.AbstractMessageLite.Builder.addAll(
          values, excludePath_);
      onChanged();
      return this;
    }
    /**
     * <pre>
     * List of file paths to exclude as inputs for a code generator.
     * a valid value start from the root package directory of the source file. Globs are supported
     * ie. google/&#42;
     * </pre>
     *
     * <code>repeated string exclude_path = 2;</code>
     */
    public Builder clearExcludePath() {
      excludePath_ = com.google.protobuf.LazyStringArrayList.EMPTY;
      bitField0_ = (bitField0_ & ~0x00000002);
      onChanged();
      return this;
    }
    /**
     * <pre>
     * List of file paths to exclude as inputs for a code generator.
     * a valid value start from the root package directory of the source file. Globs are supported
     * ie. google/&#42;
     * </pre>
     *
     * <code>repeated string exclude_path = 2;</code>
     */
    public Builder addExcludePathBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      ensureExcludePathIsMutable();
      excludePath_.add(value);
      onChanged();
      return this;
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


    // @@protoc_insertion_point(builder_scope:krotoplus.compiler.FileFilter)
  }

  // @@protoc_insertion_point(class_scope:krotoplus.compiler.FileFilter)
  private static final com.github.marcoferrer.krotoplus.config.FileFilter DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new com.github.marcoferrer.krotoplus.config.FileFilter();
  }

  public static com.github.marcoferrer.krotoplus.config.FileFilter getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<FileFilter>
      PARSER = new com.google.protobuf.AbstractParser<FileFilter>() {
    @java.lang.Override
    public FileFilter parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new FileFilter(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<FileFilter> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<FileFilter> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public com.github.marcoferrer.krotoplus.config.FileFilter getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

