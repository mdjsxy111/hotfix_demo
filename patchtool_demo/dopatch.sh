#!/usr/bin/env bash
java -jar hotfix_demo/patchtool_demo/BCFixPatchTools-1.2.0.jar -cmd patch -src_apk /Users/wuer/Projects/alibaba/hotfix/hotfix-android/hotfix_demo/patchtool_demo/hotfix_demo-old.apk -fixed_apk /Users/wuer/Projects/alibaba/hotfix/hotfix-android/hotfix_demo/patchtool_demo/hotfix_demo-new.apk -wp /Users/wuer/Projects/alibaba/hotfix/hotfix-android/hotfix_demo/patchtool_demo/patch_out -sign_file_url /Users/wuer/Projects/alibaba/hotfix/hotfix-android/hotfix_demo/demo.jks -sign_file_pass test123 -sign_alias test123 -sign_alias_pass test123 -filter_class_file /Users/wuer/Projects/alibaba/hotfix/hotfix-android/hotfix_demo/patchtool_demo/classFilter.txt
adb push /Users/wuer/Projects/alibaba/hotfix/hotfix-android/hotfix_demo/patchtool_demo/patch_out/hotfix-working/baichuan-hotfix-patch.jar /sdcard/Download