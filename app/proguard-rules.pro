# Add project specific ProGuard rules here.
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions, AnnotationDefault
-dontnote kotlinx.serialization.AnnotationsKt

# kotlinx.serialization: 保留 Companion 与 serializer 工厂
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# kotlinx.serialization: JsonElement 树解析（会话列表等动态结构）在 R8 下解析会失败，保留完整实现
-keep class kotlinx.serialization.json.JsonElement { *; }
-keep class kotlinx.serialization.json.JsonPrimitive { *; }
-keep class kotlinx.serialization.json.JsonNull { *; }
-keep class kotlinx.serialization.json.JsonArray { *; }
-keep class kotlinx.serialization.json.JsonObject { *; }
-keep class kotlinx.serialization.json.internal.** { *; }

# 保留 data/remote 下的 DTO 与 Retrofit 接口（含字段名，序列化与注解反射都需要）。
#
# 这里**不能**加 allowshrinking：DTO 只以泛型实参的形式被引用，serializer 又是反射查出来的，
# R8 会判定它们"未被使用"而整类删除，接着连 Retrofit 接口方法的泛型签名也一起退化——
# mapping 里可见 javac 签名被替换成
#   `java.lang.Object fetchConfig(kotlin.coroutines.Continuation)` + residualsignature 注解。
# 后果是 Retrofit 在发请求前就抛
#   `Unable to create converter for class java.lang.Object`
# 于是**所有**接口调用一律失败（登录、订阅、订单全灭），而界面只会显示"请求失败"。
# 该问题只在 release（开启 minify）出现，debug 构建完全正常，极易被漏掉。
# 范围仍限定在 data/remote 下，业务代码不受影响。
-keep,allowobfuscation class com.slte.app.data.remote.** { *; }
-keepclassmembers class com.slte.app.data.remote.** {
    <fields>;
}

# Retrofit: 保留接口方法签名与注解（注解驱动的反射）
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep,allowobfuscation interface retrofit2.http.** { *; }
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# maxmind-db: Reader 通过 @MaxMindDbConstructor 注解反射构造解码类，
# 混淆类名或剥离注解会抛 "No constructor ... annotation was found"（GeoIP 解析失效）
-keep class com.maxmind.db.** { *; }
-keepclassmembers class com.maxmind.db.** {
    <init>(...);
}

# Hilt 生成代码由插件处理，无需手动 keep
