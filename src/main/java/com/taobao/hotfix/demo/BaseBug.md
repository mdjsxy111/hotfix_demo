# 正确范例1. 直接修改方法
    public static void test(Context context) {
        Toast.makeText(context.getApplicationContext(), "new apk...", Toast.LENGTH_SHORT).show();
    }

# 错误范例2. 不允许直接添加/修改全局实例变量(包括静态变量), 不允许修改构造函数
最新的补丁工具会忽略掉这些情况下的变更.

新增temp全局变量

    private static String temp = "new apk...";//新增temp变量不允许!
    public static void test(Context context) {
        Toast.makeText(context.getApplicationContext(), temp, Toast.LENGTH_SHORT).show();
    }
打补丁工具直接报错, `com.taobao.baichuan.exception.HotFixException: There some Field changed`

修改全局变量(静态变量也一样)

    private static String temp = "old apk...";
    private static String temp = "new apk...";
修改构造函数

    public BaseBug() {
         //temp = "old apk..."; 修复前
         temp = "new apk..."; 修复后
    }
另一方面代码块也是不允许修改的, 也会被打补丁工具直接忽略. 包括静态代码块

    {
        Log.d(TAG, "old block");
    }
    static{
        Log.d(TAG, "new block");
    }
解释下: 其实原理类似, 总之目前的打包工具会把smail文件中的<init>, <clinit>函数的任何修改, 直接忽略.

# 错误范例4. 不允许直接添加新的方法
打补丁的时候会抛"com.taobao.android.dex.PatchException: can't add method:newMethod"异常

    public static void test(Context context) {
        newMethod(context);
    }

    public static void newMethod(Context context) {
        Toast.makeText(context.getApplicationContext(), "new apk...", Toast.LENGTH_SHORT).show();
    }

# 错误范例5. 不允许修改构造函数
假如BaseBug有构造函数, 任何对构造函数的修改都是不允许的, 会导致加载补丁失败

    public BaseBug() {
         //temp = "old apk..."; 修复前
         temp = "new apk..."; 修复后
    }

# 正确范例6. 新增类
虽然不允许直接添加属性和方法, 但是支持新增类, 可以通过新增类(可以是新增内部类)的目的来达到添加属性和方法的目的

    static class NewClass{
        static String temp = "new apk...";
        static void test(Context context) {
            Toast.makeText(context.getApplicationContext(), temp, Toast.LENGTH_SHORT).show();
        }
    }
    public static void test(Context context) {
        NewClass.test(context);
    }

# 范例7. 方法被反射调用
## 静态方法反射调用
假设test方法被反射调用, 因为静态方法反射调用可以有两种方式, 所以会出现以下两种情况, 一一说明

    Class cls = BaseBug.class;
    Method method = cls.getDeclaredMethod("test", new Class[]{Context.class});
    
### 情况1. 静态方法类调用
不支持,test方法不能被patch, 此时会报错类似`IllegalArgumentException: Expected receiver of type com.taobao.hotfix.demo.BaseBug_CF_1476949855532, but got com.taobao.hotfix.demo.BaseBug`的异常, 因为此时test方法已经被替换为BaseBug_CF_1476949855532补丁类中的test方法. 所以如果BaseBug对象invoke test方法的话, 肯定IllegalArgumentException异常


    method.invoke(new BaseBug(), new Object[]{mContext});

### 情况2. 静态方法对象调用
支持, test方法能被patch, 因为参数是null

    method.invoke(null, new Object[]{mContext});

## 非静态方法反射调用
非静态方法调用肯定是类似`method.invoke(new BaseBug(), new Object[]{});` 参考上面, 所以肯定会有IllegalArgumentException异常的, 所以此时test方法不能被patch.

## 格外注意
如果补丁加载过程中碰到`IllegalArgumentException: Expected receiver of type com.taobao.hotfix.demo.BaseBug_CF_1476949855532, but got com.taobao.hotfix.demo.BaseBug`类似的错误, 该方法多半是被反射调用了, 该方法是不支持被patch的. 可能有时候全局搜索项目, 但是没发现方法被反射调用, 那么就需要认真检查了, 是否有第三方库反射调用该方法, 比如旧版本的EvenBus框架, 通过反射调用onEventMainThread/onEvent方法实现事件总线机制, 所以onEventMainThread/onEvent方法等方法其实是不能被patch的.

# 范例8. 注解说明
## 运行时注解
假设代码中定义了这么一个运行时注解

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Inherited
    public @interface MethodInfo {
        String info();
    }
### 情况1. 修复注解本身
这样是不支持的, 此时打补丁过程会提示没有任何类的修改 

    public @interface MethodInfo {
        String info() default "old apk...";
    }

    public @interface MethodInfo {
        String info() default "new apk...";
    }
### 情况2. 修复被运行时注解的方法
test方法被MethodInfo注解, 其它地方拿到test方法的注解, 此时如果需要修复test方法MethodInfo注解信息, 是支持的. annoTest方法能够被正确patch

	//修复前 @MethodInfo(info="old apk...")
    //修复后 @MethodInfo(info="new apk...")
    public static void test(Context context {
    	Toast.makeText(context.getApplicationContext(), "old apk...", Toast.LENGTH_SHORT).show();
    }

## 编译期注解
一般情况下来说是支持的, 但是要保证编译期注解的方法, 不能被编译框架生成的三方类反射调用, 如果被反射调用了, 那么该方法不能被patch
> 比如: 常见的butterknife框架注解的类是支持的, butterknife注解修饰的方法没用被反射调用

# 错误范例9. 方法参数相关
1. 参数包括：long, double, float基本类型的方法不能被patch. 比如:test(Context context, long value), 注意这几种基本类型的封装类是支持的. 比如:test(Context context, Long value)这样是支持的
2. 参数超过8的方法不能被patch.
3. 泛型参数的方法不能被patch. 比如:test(Context context, T t);

在art虚拟机(android5.0以上),会发现其实没有这个限制, 上面两种情况可以被patch成功. 但是在dalvik虚拟机上会有兼容性问题, 所以我们标注为不支持, 请不要触发这个红线. 后续我们会持续研发改进, 取消上面几个限制, 敬请期待.

# 写在最后
上述的那些限制都不是固定的, 我们会持续研发, 逐步减少那些限制.
