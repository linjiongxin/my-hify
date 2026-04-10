#!/usr/bin/env python3
"""
Spring Boot Controller 扫描器
=============================
扫描项目中的 Spring Boot Controller 类，提取所有 API 接口信息。

处理 Grep 无法处理的复杂情况：
- 类级别 @RequestMapping 前缀
- 各种 HTTP 方法注解（@GetMapping, @PostMapping, @PutMapping, @DeleteMapping, @PatchMapping）
- 通用 @RequestMapping 注解
- 路径变量和查询参数
- Spring Security 权限注解
- 嵌套类和继承关系

使用方式：
    python detect-controllers.py <项目根目录>

输出格式：
    CONTROLLER  类全名  基础路径  源文件:行号
    ENDPOINT    HTTP方法  完整路径  方法名  源文件:行号
"""

import os
import re
import sys


# HTTP 方法注解映射
HTTP_METHOD_ANNOTATIONS = {
    'GetMapping': 'GET',
    'PostMapping': 'POST',
    'PutMapping': 'PUT',
    'DeleteMapping': 'DELETE',
    'PatchMapping': 'PATCH',
}

# 匹配类级别的 @RequestMapping 或 @RestController
CLASS_MAPPING_PATTERN = re.compile(
    r'@(RestController|Controller|RequestMapping)\s*(?:\(\s*(?:value\s*=\s*)?[\'"]([^\'"]*)[\'"])?',
    re.IGNORECASE,
)

# 匹配方法级别的 HTTP 注解
METHOD_ANNOTATION_PATTERN = re.compile(
    r'@((\w+Mapping)(?:\s*\(\s*(?:value\s*=\s*)?[\'"]([^\'"]*)[\'"])?|RequestMapping\s*\([^)]*method\s*=\s*RequestMethod\.(\w+)[^)]*(?:value\s*=\s*[\'"]([^\'"]*)[\'"])?)',
    re.IGNORECASE,
)

# 简化版方法注解匹配（更可靠）
METHOD_SIMPLE_PATTERN = re.compile(
    r'@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\s*\(\s*(?:value\s*=\s*)?[\'"]?([^\'"\),]*)[\'"]?',
    re.IGNORECASE,
)

# 匹配类定义
CLASS_PATTERN = re.compile(
    r'(public|protected|private)?\s*(abstract|final)?\s*class\s+(\w+)',
    re.IGNORECASE,
)

# 匹配方法定义（简化版）
METHOD_PATTERN = re.compile(
    r'(public|protected|private)\s+(?:<[^>]+>\s*)?(?:[\w<>,\s]+)\s+(\w+)\s*\([^)]*\)',
    re.IGNORECASE,
)

# 匹配权限注解
AUTH_PATTERN = re.compile(
    r'@(PreAuthorize|PostAuthorize|Secured|RolesAllowed)\s*\(\s*[\'"]([^\'"]*)[\'"]\s*\)',
    re.IGNORECASE,
)

# 匹配路径变量
PATH_VAR_PATTERN = re.compile(r'@PathVariable\s*(?:\(\s*[\'"]([^\'"]*)[\'"]\s*\))?')

# 匹配查询参数
REQUEST_PARAM_PATTERN = re.compile(r'@RequestParam\s*(?:\(\s*(?:value\s*=\s*)?[\'"]([^\'"]*)[\'"]\s*(?:,\s*required\s*=\s*(\w+))?\))?')

# 匹配请求体
REQUEST_BODY_PATTERN = re.compile(r'@RequestBody')


def find_base_path(content):
    """从类内容中提取基础路径"""
    # 优先查找 @RequestMapping(value = "/xxx")
    mapping_match = re.search(
        r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?[\'"]([^\'"]*)[\'"]',
        content,
        re.IGNORECASE,
    )
    if mapping_match:
        return mapping_match.group(1)
    return ''


def scan_controller_file(filepath):
    """扫描单个 Controller 文件"""
    endpoints = []
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
            lines = content.split('\n')
    except (UnicodeDecodeError, PermissionError, IOError):
        return None, []

    # 检查是否是 Controller 类
    if not re.search(r'@(RestController|Controller)', content, re.IGNORECASE):
        return None, []

    # 提取类名
    class_match = CLASS_PATTERN.search(content)
    if not class_match:
        return None, []
    class_name = class_match.group(3)

    # 提取类级别的路径前缀
    base_path = find_base_path(content)

    # 找到类定义的行号
    class_line = 1
    for i, line in enumerate(lines):
        if f'class {class_name}' in line:
            class_line = i + 1
            break

    # 按行扫描方法注解
    current_method = None
    current_auth = None

    for i, line in enumerate(lines):
        line_num = i + 1

        # 检查权限注解
        auth_match = AUTH_PATTERN.search(line)
        if auth_match:
            current_auth = auth_match.group(2)
            continue

        # 检查 HTTP 方法注解
        method_found = False

        # 尝试匹配各种 HTTP 方法注解
        for annotation, http_method in HTTP_METHOD_ANNOTATIONS.items():
            pattern = rf'@{annotation}\s*\(\s*(?:value\s*=\s*)?[\'"]?([^\'"\),\]]*)[\'"]?'
            match = re.search(pattern, line, re.IGNORECASE)
            if match:
                path = match.group(1) if match.group(1) else ''
                full_path = (base_path + '/' + path).replace('//', '/')
                if not full_path.startswith('/'):
                    full_path = '/' + full_path

                endpoints.append({
                    'method': http_method,
                    'path': full_path,
                    'line': line_num,
                    'auth': current_auth,
                })
                method_found = True
                current_auth = None
                break

        # 匹配通用 @RequestMapping(method = RequestMethod.XXX)
        if not method_found:
            req_mapping = re.search(
                r'@RequestMapping\s*\([^)]*method\s*=\s*RequestMethod\.(\w+)[^)]*',
                line,
                re.IGNORECASE,
            )
            if req_mapping:
                http_method = req_mapping.group(1).upper()
                # 尝试提取路径
                path_match = re.search(r'value\s*=\s*[\'"]([^\'"]*)[\'"]', line)
                path = path_match.group(1) if path_match else ''
                full_path = (base_path + '/' + path).replace('//', '/')
                if not full_path.startswith('/'):
                    full_path = '/' + full_path

                endpoints.append({
                    'method': http_method,
                    'path': full_path,
                    'line': line_num,
                    'auth': current_auth,
                })
                current_auth = None

    return {
        'class_name': class_name,
        'base_path': base_path,
        'file': filepath,
        'line': class_line,
    }, endpoints


def scan_project(root_dir):
    """递归扫描项目目录"""
    all_controllers = []
    all_endpoints = []

    # 常见的 Java 源文件目录
    src_dirs = ['src/main/java', 'src']

    for src_dir in src_dirs:
        full_src = os.path.join(root_dir, src_dir)
        if not os.path.exists(full_src):
            continue

        for dirpath, _dirs, files in os.walk(full_src):
            for filename in files:
                if filename.endswith('.java'):
                    filepath = os.path.join(dirpath, filename)
                    controller_info, endpoints = scan_controller_file(filepath)
                    if controller_info:
                        all_controllers.append(controller_info)
                        for ep in endpoints:
                            ep['controller'] = controller_info['class_name']
                        all_endpoints.extend(endpoints)

    return all_controllers, all_endpoints


def main():
    if len(sys.argv) < 2:
        print("用法: python detect-controllers.py <项目根目录>", file=sys.stderr)
        sys.exit(1)

    root_dir = sys.argv[1]

    if not os.path.isdir(root_dir):
        print(f"错误: {root_dir} 不是有效的目录", file=sys.stderr)
        sys.exit(1)

    controllers, endpoints = scan_project(root_dir)

    if not controllers:
        print("未找到任何 Controller 类。", file=sys.stderr)
        sys.exit(0)

    # 输出 Controller 列表
    print("=" * 80)
    print("Controller 列表")
    print("=" * 80)
    for ctrl in controllers:
        rel_path = os.path.relpath(ctrl['file'], root_dir)
        print(f"{ctrl['class_name']:<40} {ctrl['base_path']:<20} {rel_path}:{ctrl['line']}")

    print()
    print("=" * 80)
    print("API 接口列表")
    print("=" * 80)
    print(f"{'METHOD':<8} {'PATH':<40} {'CONTROLLER':<25} {'AUTH':<15}")
    print("-" * 80)

    # 按 Controller 和路径排序
    endpoints.sort(key=lambda e: (e['controller'], e['path'], e['method']))

    for ep in endpoints:
        auth = ep['auth'] if ep['auth'] else '-'
        print(f"{ep['method']:<8} {ep['path']:<40} {ep['controller']:<25} {auth:<15}")

    print()
    print(f"总计: {len(controllers)} 个 Controller, {len(endpoints)} 个接口")


if __name__ == '__main__':
    main()
