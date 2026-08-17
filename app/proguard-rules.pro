# Мост из JS в Kotlin вызывается по имени метода через addJavascriptInterface,
# поэтому оптимизатор не должен его переименовывать.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
