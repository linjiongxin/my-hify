---
name: commit
description: 快速 git 提交，支持自动生成或指定提交信息
argument-hint: [可选: 提交信息]
disable-model-invocation: true
allowed-tools: Bash(git status:*), Bash(git add:*), Bash(git commit:*), Bash(git diff:*)
model: haiku
---

创建 git 提交。

如果提供了提交信息：$ARGUMENTS
- 使用该内容作为提交信息

如果未提供提交信息：
- 使用 `git diff --staged`（如果没有暂存则用 `git diff`）分析变更
- 生成简洁、有意义的提交信息

## 步骤

1. 使用 `git status` 查看当前状态
2. 如果没有暂存内容，运行 `git add .` 暂存所有变更
3. 使用 `git diff --staged` 查看将要提交的内容
4. 创建提交：
    - 如果提供了 `$ARGUMENTS`，使用它作为提交信息
    - 否则，基于 diff 生成提交信息
5. 显示提交结果

## 提交信息格式

- 以类型开头：`feat:`、`fix:`、`docs:`、`refactor:`、`test:`、`chore:`
- 简洁但具描述性（首行最多 72 个字符）
- 示例：`feat: 添加 JWT 用户认证`

## 输出

显示简短确认信息：
✓ 已提交：[提交信息] [数量] 个文件变更
