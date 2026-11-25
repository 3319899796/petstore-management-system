# -*- coding: utf-8 -*-
"""
前端 ↔ 后端 契约自检（不需要 node / npm，纯文本比对）

检查三件事：
1. src/api/index.js 里每个请求的「方法 + 路径」在后端 Controller 里真的存在；
2. 每个页面 import 的接口函数在 src/api/index.js 里真的导出过；
3. 页面里用到的权限码，在 schema.sql 的 role 权限串或 docs/接口清单.md 里有出处。

用法：python tools/check_frontend.py
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BACKEND = ROOT / "src" / "backend" / "src" / "main" / "java" / "com" / "ningcong" / "petstore"
FRONTEND = ROOT / "src" / "frontend" / "src"
API_JS = FRONTEND / "api" / "index.js"
SCHEMA = ROOT / "src" / "db" / "schema.sql"
CONTRACT = ROOT / "docs" / "接口清单.md"

problems = []
infos = []


def norm(path: str) -> str:
    """把 ${xx} / {xx} 统一成 {}，去掉多余斜杠"""
    path = re.sub(r"\$\{[^}]*\}", "{}", path)
    path = re.sub(r"\{[^}]*\}", "{}", path)
    path = re.sub(r"/+", "/", "/" + path.strip("/"))
    return path


def backend_routes():
    routes = set()
    for path in (BACKEND / "controller").glob("*.java"):
        text = path.read_text(encoding="utf-8")
        base_m = re.search(r'@RequestMapping\("([^"]*)"\)', text)
        base = base_m.group(1) if base_m else ""
        for m in re.finditer(r'@(Get|Post|Put|Delete)Mapping(?:\("([^"]*)"\))?', text):
            verb = m.group(1).upper()
            sub = m.group(2) or ""
            routes.add((verb, norm(base + "/" + sub if sub else base)))
    return routes


def frontend_routes():
    text = API_JS.read_text(encoding="utf-8")
    out = set()
    for m in re.finditer(r"request\.(get|post|put|delete)\(\s*[`'\"]([^`'\"]+)[`'\"]", text):
        out.add((m.group(1).upper(), norm(m.group(2))))
    return out


def exported_api():
    text = API_JS.read_text(encoding="utf-8")
    return set(re.findall(r"export function (\w+)", text))


def main():
    be, fe = backend_routes(), frontend_routes()
    # /demo/* 是给测试用的接口（演示越权 403），前端故意不接
    be = {r for r in be if not r[1].startswith("/demo")}
    for verb, path in sorted(fe):
        if (verb, path) not in be:
            problems.append(f"[接口] 前端调了 {verb} {path}，后端没有这个映射")
    for verb, path in sorted(be):
        if (verb, path) not in fe:
            problems.append(f"[接口] 后端有 {verb} {path}，前端没写对应函数（可能是漏做）")

    exported = exported_api()
    for path in FRONTEND.rglob("*.vue"):
        text = path.read_text(encoding="utf-8")
        for m in re.finditer(r"import\s*\{([^}]+)\}\s*from\s*['\"]@/api['\"]", text):
            for name in [n.strip() for n in m.group(1).split(",") if n.strip()]:
                if name not in exported:
                    problems.append(f"[import] {path.name} 引入了 {name}，api/index.js 没导出")

    # 权限码：页面里 hasPerm('xxx:yyy') / hasAnyPerm(['a','b'])
    schema_text = SCHEMA.read_text(encoding="utf-8")
    contract_text = CONTRACT.read_text(encoding="utf-8") if CONTRACT.exists() else ""
    known = set(re.findall(r"[a-z]+:[a-z]+", schema_text)) | set(re.findall(r"[a-z]+:[a-z]+", contract_text))
    code_in_fe = set()
    for path in list(FRONTEND.rglob("*.vue")) + list(FRONTEND.rglob("*.js")):
        text = path.read_text(encoding="utf-8")
        code_in_fe |= set(re.findall(r"['\"]([a-z]+:[a-z]+)['\"]", text))
    for code in sorted(code_in_fe - known):
        problems.append(f"[权限] 前端用了权限码 {code}，schema.sql / 接口清单里查不到")
    infos.append(f"接口：后端 {len(be)} 个映射，前端 {len(fe)} 个请求")
    infos.append(f"权限码：前端用到 {len(code_in_fe)} 个")

    print("\n".join("· " + i for i in infos))
    if problems:
        print(f"\n发现 {len(problems)} 个问题：")
        for p in problems:
            print(" - " + p)
        sys.exit(1)
    print("\n前后端契约一致，没有发现问题。")


if __name__ == "__main__":
    main()
