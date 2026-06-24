# Router.init() loads this entry point by its stable name; renaming it would break initialization.
# Router.init() 通过稳定类名加载该入口；重命名会导致初始化失败。
-keep class com.oayilix.lodestar.mapping.RouterMapping {
    public static java.util.Map get();
}
