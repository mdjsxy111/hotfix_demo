# 产品体验
patchtool_demo目录下已经有apk和补丁包. 补丁包修复如下

    public class BaseBug {
        public static void test(Context context) {
            //修复后
            Toast.makeText(context.getApplicationContext(), "old apk...", Toast.LENGTH_SHORT).show();
            //修复后
            //Toast.makeText(context.getApplicationContext(), "new apk...", Toast.LENGTH_SHORT).show();
        }
    }
## 应用本地补丁示范
1. 安装hotfix_demo-old.apk, 运行然后点击测试按钮. toast提示"old apk..."
2. 安装调试工具, 输入包名`com.taobao.hotfix.demo`. 然后点击连接应用
3. 推送补丁包到本地任何一个目录. 比如:`adb push patchtool_demo/patch_out/hotfix-working/baichuan-hotfix-patch.jar /sdcard/Download`
4. 调试工具应用本地补丁按钮上面那个输入框填写: `/sdcard/Download/baichuan-hotfix-patch.jar`, 然后点击应用本地补丁按钮
5. 调试工具如果显示"Code:1 Info:load success"日志, 则表明补丁加载成功.
6. 切换回原应用, 点击测试按钮, toast提示"new apk...". 补丁生效

## 扫描二维码示范
1. 修改demo项目, 修改appid, appkey, appsecret, rsasecret参数为自己项目的
2. 补丁工具打补丁包
3. 上传补丁包到百川后台
4. 调试工具扫码验证

# Demo使用说明
1. BaseBug.java  -- 修复前
2. BaseBug.md   -- 修复后, 请详细阅读此文件中的说明

## patchtool_demo目录说明
1. 把BCFixPatchTools-1.2.0.jar放入该目录下
2. 新建一个patch_out文件夹作为最后补丁输出目录
3. 把原apk和修复后的apk也放置到该目录中, 打包命令参考(debug: `./gradlew clean assembleDebug` release:`./gradlew clean assembleRelease`), ps:请勿直接使用AS即时编译Instance Run出的apk产物进行打补丁操作.
4. hotfix_demo目录下执行以下命令

	```
    java -jar hotfix_demo/patchtool_demo/BCFixPatchTools-1.2.0.jar -cmd patch -src_apk /Users/wuer/Projects/alibaba/hotfix/hotfix-android/hotfix_demo/patchtool_demo/hotfix_demo-old.apk -fixed_apk /Users/wuer/Projects/alibaba/hotfix/hotfix-android/hotfix_demo/patchtool_demo/hotfix_demo-new.apk -wp /Users/wuer/Projects/alibaba/hotfix/hotfix-android/hotfix_demo/patchtool_demo/patch_out -sign_file_url /Users/wuer/Projects/alibaba/hotfix/hotfix-android/hotfix_demo/demo.jks -sign_file_pass test123 -sign_alias test123 -sign_alias_pass test123 -filter_class_file /Users/wuer/Projects/alibaba/hotfix/hotfix-android/hotfix_demo/patchtool_demo/classFilter.txt
	```
   命令仅供参考, 使用时需要替换为自己项目相关的路径, 注意使用绝对路径而不是相对路径! 查看命令参数帮助`java -jar BCFixPatchTools-1.2.0.jar -cmd help`

   正常情况下, hotfix-working目录下将会有

   * baichuan-hotfix-patch-unsigned.jar --> 未签名补丁包
   * baichuan-hotfix-patch.jar -->签名补丁包 (如果没有, 那么补丁签名失败)

   如果没加`-sign_file_url`等签名相关参数, 那么只会生成`baichuan-hotfix-patch.jar`, 此时补丁也是未签名的!

## 打补丁过程日志分析
```
<init> is filtered
<clinit> is filtered
```
如果有上面的提示, 就说明直接忽略了构造函数的修改或者全局变量(静态变量)的修改.

```
add modified Method:V  test(Landroid/content/Context;)  in Class:Lcom/taobao/hotfix/demo/BaseBug;
addModifiedClass: com.taobao.hotfix.demo.BaseBug
template: Lcom/taobao/hotfix/demo/BaseBug;->test
....
```
`add modified Method` 这里表明的就是你代码中要修复的方法, 如果这里有多余的不是你修改的方法.
那么请务必检查

1. apk前后混淆配置是否一样
2. 检查如果是这里看到多余的第三库类方法修改, 那么可以尝试在打补丁命令中增加`-filter_class_file`参数, 包含在此文件中的类, 将不会打入patch，直接过滤掉类.

如果应用了混淆配置
```
add modified Method:V  a(Landroid/content/Context;)  in Class:Lcom/taobao/hotfix/demo/a;
```
查找mapping.txt, 查找对应关系, 可以发现com/taobao/hotfix/demo/a.a(Context context)就是BaseBug.test(Context context)方法
```
com.taobao.hotfix.demo.BaseBug -> com.taobao.hotfix.demo.a:
    void test(android.content.Context) -> a
```

如果有下面的提示, 说明前后apk没发生任何变化.
```
Error in Patch, please check info below: java.lang.Exception: No apatch files! No classes modify!
```

# so文件添加说明
本demo中libs下的libtest.so文件只是测试使用, 同时可以看到项目目录结构中包含有armeabi-v7a/arm64-v8a目录, 所以需要把手动把下载sdk中得到的armeabi-v7a/arm64-v8a目录下的libandfix.so文件拷贝进来.
如果没有的话, 则不需要做拷贝处理. x86, x86_64不需要这样处理, hotfix默认不支持这两种架构CPU.

# 高级调试技巧
1. HotFixManager.initialize方法isDebugable参数设置为true, 查看hotfix相关日志, 过滤关键字: BCHotfix(区分大小写). debug版本默认会在控制台输出log. release版本可以通过`adb logcat -v time | grep "BCHotfix"`在命令行中执行查看
2. 补丁工具是通过比较前后apk中的dex文件差异来打的补丁包, 所以可以把前后apk反编译为smail文件(apktool d **.apk), 然后通过常见的文本比较工具(beyond compare)来比较差异.
3. 查看补丁包是否正确被打入

   反编译baichuan-hotfix-patch.jar补丁包.

   ```
   apktool d baichuan-hotfix-patch.jar
   ```
> 2.3点中查看smail文件, 看下需要修复的方法是否被正确打入且不能包含多余方法的修改. 如果包含了发现多余无关的方法被打入到了补丁包中, 首先检查apk前后混淆配置是否一样, 然后如果是第三库类方法, 那么可以尝试在打补丁命令中增加`-filter_class_file`参数, 包含在此文件中的类, 将不会打入patch，直接过滤掉类.

文档参考:

1. [smail语法](http://blog.isming.me/2015/01/14/android-decompile-smali/)












