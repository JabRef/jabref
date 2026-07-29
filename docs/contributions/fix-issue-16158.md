# 修复 Issue #16158：优化"搜索未链接文件"对话框

## 背景

JabRef 的"搜索未链接文件"向导存在 5 个用户体验问题：

1. 对话框未使用 JabRef 的 CSS 样式
2. 窗口右上角的问号按钮应移除
3. 下拉菜单显示"Default"不合适
4. 搜索文件时不应显示"Select files to import"标题
5. 树形结构中空格键应勾选复选框（可能是 JavaFX 上游问题）

## 修复内容

### 1. 对话框未使用 JabRef 的 CSS 样式

**文件**: `jabgui/src/main/java/org/jabref/gui/externalfiles/UnlinkedFilesWizard.java`

**修改**: 在 Wizard 显示时应用 JabRef CSS 样式

```java
// Issue #16158: Apply JabRef CSS to the wizard dialog
wizard.setOnShown(event -> {
    if (wizard.getDialogPane() != null && wizard.getDialogPane().getScene() != null) {
        themeManager.installCssOnScene(wizard.getDialogPane().getScene());
    }
});
```

### 2. 移除帮助按钮

**文件**: `jabgui/src/main/java/org/jabref/gui/externalfiles/UnlinkedFilesWizard.java`

**修改**: 调用 `setShowHelpButton(false)` 隐藏帮助按钮

```java
// Issue #16158: Remove the help button since it doesn't link to a help page
wizard.setShowHelpButton(false);
```

### 3. 修改下拉菜单文本

**文件**: `jablib/src/main/java/org/jabref/logic/externalfiles/ExternalFileSorter.java`

**修改**: 将 "Default" 改为 "Grouped by directory"

```java
public enum ExternalFileSorter {
    DEFAULT(Localization.lang("Grouped by directory")),
    // ...
}
```

### 4. 搜索时隐藏标题

**文件**: `jabgui/src/main/java/org/jabref/gui/externalfiles/FileSelectionPage.java`

**修改**: 
- 添加 `setHeaderVisible(boolean)` 方法
- 在 `onEnteringPage` 中隐藏标题
- 在搜索完成后恢复标题

```java
public void setHeaderVisible(boolean visible) {
    if (visible) {
        setHeaderText(Localization.lang("Select files to import"));
    } else {
        setHeaderText(null);
    }
}
```

### 5. 空格键勾选复选框

**状态**: 这可能是 JavaFX 上游问题，需要创建最小可复现示例验证。

## 修改文件清单

| 文件 | 修改内容 |
|------|----------|
| `UnlinkedFilesWizard.java` | 添加 ThemeManager 注入、移除帮助按钮、应用 CSS |
| `FileSelectionPage.java` | 添加 setHeaderVisible 方法、搜索时隐藏标题 |
| `ExternalFileSorter.java` | 修改 DEFAULT 文本为 "Grouped by directory" |

## 验证方法

1. 运行 JabRef 应用程序
2. 打开"搜索未链接文件"对话框
3. 验证：
   - 对话框使用 JabRef 主题样式
   - 没有帮助按钮
   - 排序下拉菜单显示 "Grouped by directory"
   - 搜索时标题隐藏
