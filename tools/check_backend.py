"""
静态自检脚本：在没有 Maven / 数据库的环境下，尽量提前发现"代码之间对不上"的低级错误。

检查四件事：
1. Java 源码里 import 的 com.ningcong.petstore.* 类，文件是否真的存在（防止写错包名/类名）。
2. 每个 Mapper 接口里声明的方法，是否都能在源码里找到对应的调用点（反过来也查：调用了却不存在的方法）。
3. @RequirePerm("xxx") 里的权限码，是否出现在 schema.sql 的 role 表权限串里（防止拼错导致永远 403）。
4. 实体类的类名/字段是否和 schema.sql 的列对得上（驼峰 ↔ 下划线）。

运行：python tools/check_backend.py
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
JAVA_ROOT = ROOT / "src" / "backend" / "src" / "main" / "java"
SCHEMA = ROOT / "src" / "db" / "schema.sql"

problems = []
infos = []


def java_files():
    return list(JAVA_ROOT.rglob("*.java"))


def check_imports():
    """1. import 的本地类是否存在"""
    for path in java_files():
        text = path.read_text(encoding="utf-8")
        for m in re.finditer(r"^import\s+(com\.ningcong\.petstore\.[\w.]+);", text, re.M):
            fqcn = m.group(1)
            rel = fqcn.replace(".", "/") + ".java"
            target = JAVA_ROOT / rel
            if not target.exists():
                problems.append(f"[import] {path.name}: 找不到 {fqcn}")


def parse_mapper_methods():
    """2. Mapper 接口里声明的方法名"""
    declared = {}
    for path in (JAVA_ROOT / "com" / "ningcong" / "petstore" / "mapper").glob("*.java"):
        text = path.read_text(encoding="utf-8")
        name = path.stem
        methods = set()
        # 匹配形如  List<Xxx> foo(...);  或  Xxx foo(...);
        for m in re.finditer(r"^\s{4}(?:public\s+)?[\w<>,\[\]\. ]+\s+(\w+)\s*\(", text, re.M):
            methods.add(m.group(1))
        declared[name] = methods
    return declared


def check_mapper_calls(declared):
    """2. 调用点 vs 声明（只看自定义 Mapper，BaseMapper 的方法跳过）"""
    base_methods = {
        "selectById", "selectList", "selectOne", "selectCount", "selectPage", "selectBatchIds",
        "insert", "updateById", "deleteById", "delete", "update", "selectMaps", "exists",
    }
    for path in java_files():
        if "/mapper/" in path.as_posix():
            continue
        text = path.read_text(encoding="utf-8")
        # 变量名约定：petMapper / vaccineMapper ...
        for m in re.finditer(r"(\w+Mapper)\.(\w+)\s*\(", text):
            mapper, method = m.group(1), m.group(2)
            # 转回接口名：petMapper -> PetMapper
            iface = mapper[0].upper() + mapper[1:]
            if iface not in declared:
                continue
            if method in base_methods:
                continue
            if method not in declared[iface]:
                problems.append(f"[mapper] {path.name}: 调用了 {iface}.{method}()，但接口里没有声明")


def check_permission_codes():
    """3. @RequirePerm 的权限码是否有出处

    判定规则：管理员角色的权限是 `*`（全部权限），所以"只在管理员接口上出现"的权限码
    不会写进 role 表的权限串 —— 这是正常的。因此这里把"出处"扩大到两处：
      a) schema.sql 的 role 表权限串（工作人员 / 宠物主人用到的码）
      b) docs/接口清单.md（文档里列出的接口权限，含管理员专属）
    两边都找不到的码，才说明是拼写错误，会导致该接口永远 403。
    """
    schema = SCHEMA.read_text(encoding="utf-8")
    in_role = set()
    for m in re.finditer(r"'([\w:,\*]+)'", schema):
        for code in m.group(1).split(","):
            in_role.add(code.strip())

    doc = ROOT / "docs" / "接口清单.md"
    in_doc = set()
    if doc.exists():
        in_doc = set(re.findall(r"\b((?:pet|owner|vaccine|medical|foster|adoption|notice|user|stat):[\w]+)\b",
                                doc.read_text(encoding="utf-8")))

    used = {}
    for path in java_files():
        text = path.read_text(encoding="utf-8")
        for m in re.finditer(r"@RequirePerm\(\s*\{?([^)}]*)\}?\s*\)", text):
            for code in re.findall(r'"([\w:*]+)"', m.group(1)):
                used.setdefault(code, set()).add(path.name)

    admin_only = []
    for code, files in sorted(used.items()):
        if code in in_role:
            continue
        if code in in_doc:
            admin_only.append(code)
            continue
        problems.append(
            f"[perm] {', '.join(sorted(files))}: 权限码 {code} 既不在 role 权限串里，"
            f"也不在 docs/接口清单.md 里（拼错了会导致该接口永远 403）"
        )
    infos.append(f"权限码：代码用到 {len(used)} 个，role 表覆盖 {len(used) - len(admin_only)} 个，"
                 f"仅管理员可用 {len(admin_only)} 个（{', '.join(admin_only) or '无'}）")


def check_entity_columns():
    """4. 实体字段 vs 表列（只做数量级检查，避免误报太多）"""
    schema = SCHEMA.read_text(encoding="utf-8")
    tables = {}
    for m in re.finditer(r"CREATE TABLE (\w+) \((.*?)\n\) ENGINE", schema, re.S):
        table, body = m.group(1), m.group(2)
        cols = set()
        for line in body.splitlines():
            line = line.strip()
            if not line or line.startswith("--") or line.upper().startswith(("PRIMARY", "UNIQUE", "KEY", "CONSTRAINT", "FOREIGN")):
                continue
            col = line.split()[0]
            if re.fullmatch(r"\w+", col):
                cols.add(col)
        tables[table] = cols

    entity_dir = JAVA_ROOT / "com" / "ningcong" / "petstore" / "entity"
    for path in entity_dir.glob("*.java"):
        text = path.read_text(encoding="utf-8")
        tm = re.search(r'@TableName\("(\w+)"\)', text)
        if not tm:
            problems.append(f"[entity] {path.name}: 缺少 @TableName")
            continue
        table = tm.group(1)
        if table not in tables:
            problems.append(f"[entity] {path.name}: @TableName(\"{table}\") 在 schema.sql 里没有这张表")
            continue
        cols = tables[table]
        for fm in re.finditer(r"^\s+private\s+[\w<>,\[\]\. ]+\s+(\w+);", text, re.M):
            field = fm.group(1)
            snake = re.sub(r"(?<!^)(?=[A-Z])", "_", field).lower()
            if snake not in cols and field not in cols:
                problems.append(f"[entity] {path.name}: 字段 {field}（{snake}）在表 {table} 里找不到对应列")


def check_services_implemented():
    """额外：Controller 注入的 Service 是否都有实现类，调用的方法是否真的存在"""
    svc_dir = JAVA_ROOT / "com" / "ningcong" / "petstore" / "service"
    names = {p.stem for p in svc_dir.glob("*.java")}

    # 收集每个 Service 的 public 方法名
    public_methods = {}
    for path in svc_dir.glob("*.java"):
        text = path.read_text(encoding="utf-8")
        methods = set()
        for m in re.finditer(r"^\s{4}public\s+[\w<>,\[\]\.\? ]+\s+(\w+)\s*\(", text, re.M):
            methods.add(m.group(1))
        public_methods[path.stem] = methods

    for path in JAVA_ROOT.rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        for m in re.finditer(r"private\s+final\s+(\w+Service)\s+", text):
            if m.group(1) not in names:
                problems.append(f"[service] {path.name}: 注入了 {m.group(1)}，但 service 目录下没有这个类")
        # 调用点检查：xxxService.foo(
        for m in re.finditer(r"\b(\w+Service)\.(\w+)\s*\(", text):
            svc, method = m.group(1), m.group(2)
            if svc not in public_methods:
                continue
            if method not in public_methods[svc]:
                problems.append(f"[service] {path.name}: 调用了 {svc}.{method}()，但该类没有这个 public 方法")
    infos.append("Service 调用点：已逐个核对方法名")


def main():
    check_imports()
    declared = parse_mapper_methods()
    check_mapper_calls(declared)
    check_permission_codes()
    check_entity_columns()
    check_services_implemented()

    print(f"扫描 Java 文件 {len(java_files())} 个，schema 表 {len(re.findall(r'CREATE TABLE', SCHEMA.read_text(encoding='utf-8')))} 张")
    for line in infos:
        print("· " + line)
    if problems:
        print(f"\n发现 {len(problems)} 个问题：")
        for p in problems:
            print("  - " + p)
        return 1
    print("\n没有发现问题。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
