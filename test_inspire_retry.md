# INSPIRE Fetcher 测试指南

## 测试 Issue #12292 - 3.2 部分（重试机制和验证）

### 测试用例 1：正常 arXiv 搜索
**目标**：验证能成功获取数据
```
输入：arXiv 编号 2409.15408
期望：
- 成功获取数据
- 有日志输出：INFO "Fetching from INSPIRE..."
- 有日志输出：INFO "Successfully fetched X entries"
```

### 测试用例 2：网络重试
**目标**：验证重试机制
```
模拟：网络超时
期望：
- 自动重试 3 次
- 日志显示：WARN "Fetch attempt 1 failed"
- 日志显示：INFO "Retrying in 1000 ms..."
- 日志显示：INFO "Retrying in 2000 ms..."
```

### 测试用例 3：结果验证
**目标**：验证 citation key 质量检查
```
场景 A：返回正常 citation key
期望：INFO "Got valid citation key: 'Author:2024abc'"

场景 B：返回 URL 形式的 key
期望：WARN "Entry has URL-like citation key: 'https://...'"

场景 C：返回超长 key
期望：WARN "Entry has unusually long citation key (150 chars)"
```

### 测试用例 4：日志完整性
**期望的日志输出**：
```
[DEBUG] Using INSPIRE arXiv endpoint for: arXiv:2409.15408
[INFO]  Fetching from INSPIRE (attempt 1/3): https://... [arXiv:2409.15408]
[INFO]  Successfully fetched 1 entries from INSPIRE for [arXiv:2409.15408]
[DEBUG] Post-cleanup citation key: Author:2024abc
[INFO]  Got valid citation key: 'Author:2024abc' [arXiv:2409.15408]
[DEBUG] Entry [arXiv:2409.15408] includes journal publication info
```

## 如何测试

### 方法 1：IntelliJ 中运行
1. 在 IntelliJ 中打开项目
2. 运行 JabRef
3. 打开 Web Search，选择 INSPIRE
4. 搜索 arXiv 编号
5. 查看控制台日志

### 方法 2：查看日志文件
JabRef 的日志文件通常在：
- macOS: `~/Library/Logs/JabRef/`
- Windows: `%APPDATA%\JabRef\logs\`
- Linux: `~/.local/share/JabRef/logs/`

### 方法 3：单元测试
```java
@Test
void testArxivFetchWithRetry() throws Exception {
    BibEntry entry = new BibEntry();
    entry.setField(StandardField.ARCHIVEPREFIX, "arXiv");
    entry.setField(StandardField.EPRINT, "2409.15408");
    
    List<BibEntry> results = fetcher.performSearch(entry);
    
    assertFalse(results.isEmpty());
    assertTrue(results.get(0).hasCitationKey());
    
    // 检查 citation key 不是 URL
    String citationKey = results.get(0).getCitationKey().orElse("");
    assertFalse(citationKey.startsWith("http"));
    assertTrue(citationKey.length() < 100);
}
```

## 与团队协作测试

### 与同学 A（Core Logic）协作：
```
你的输出 → 同学A的输入
验证数据质量 → 提取 texkeys
```

### 与同学 C（Routing）协作：
```
同学C路由到INSPIRE → 你的重试机制生效
```

### 与同学 D（Cleanup）协作：
```
你标记不良键 → 同学D清理
```

## 成功标准

✅ 重试机制工作正常（3次重试，指数退避）
✅ 超时控制生效（不会卡死）
✅ 日志输出完整（INFO/WARN/DEBUG）
✅ 能识别不良 citation key（URL、超长）
✅ 错误提示友好

## 问题排查

如果测试失败：
1. 检查网络连接
2. 查看日志文件
3. 验证 INSPIRE API 是否可访问
4. 确认 arXiv 编号格式正确

