import json
import random

surnames = ["张", "李", "王", "刘", "陈", "杨", "赵", "黄", "周", "吴", "徐", "孙", "胡", "朱", "高", "林", "何", "郭", "马", "罗", "梁", "宋", "郑", "谢", "韩", "唐", "冯", "于", "董", "萧", "程", "曹", "袁", "邓", "许", "傅", "沈", "曾", "彭", "吕", "苏", "卢", "蒋", "蔡", "贾", "丁", "魏", "薛", "叶", "阎"]
given_names = ["伟", "芳", "娜", "敏", "静", "秀英", "丽", "强", "磊", "军", "洋", "勇", "艳", "杰", "娟", "涛", "明", "超", "秀兰", "霞", "平", "刚", "桂英", "玉兰", "小燕", "桂芳", "秀珍", "海燕", "春梅", "建华", "国强", "秀华", "建国", "文静", "文杰", "文博", "文轩", "子涵", "欣怡", "梓涵", "浩然", "诗涵", "一诺", "依诺", "俊驰", "宇航", "浩宇", "欣妍", "雨桐", "子萱", "梓萱", "诗雅", "思睿", "铭轩", "天宇", "梦洁", "嘉怡", "子墨", "思源", "雨泽", "博文", "皓轩", "子轩", "梓豪", "子豪", "佳怡", "思彤", "紫萱", "雨欣", "可馨", "欣怡", "梓睿", "雨欣", "子睿", "梓睿", "思睿", "子睿"]

positions_tech = ["高级工程师", "工程师", "初级工程师", "架构师", "技术专家", "开发工程师", "前端工程师", "后端工程师", "移动端工程师", "算法工程师", "数据工程师", "运维工程师", "测试工程师", "自动化测试工程师", "安全工程师", "系统工程师", "网络工程师", "数据库工程师"]
positions_market = ["销售代表", "高级销售代表", "销售经理", "市场专员", "市场经理", "品牌专员", "品牌经理", "推广专员", "推广经理", "客户经理", "大客户经理", "渠道经理", "商务专员", "商务经理"]
positions_hr = ["人力资源专员", "人力资源经理", "招聘专员", "培训专员", "薪酬专员", "绩效专员", "员工关系专员", "人事主管", "人事经理"]
positions_finance = ["会计", "出纳", "财务专员", "财务经理", "审计专员", "税务专员", "成本会计", "管理会计", "财务分析师"]
positions_general = ["行政专员", "行政经理", "前台", "文员", "秘书", "助理", "总经理助理", "办公室主任"]

departments = [
    {"id": "DEPT_001", "name": "总经理办公室", "parentId": None, "level": 1, "description": "公司最高管理层"},
    {"id": "DEPT_002", "name": "技术部", "parentId": "DEPT_001", "level": 2, "description": "负责产品研发和技术支持"},
    {"id": "DEPT_003", "name": "市场部", "parentId": "DEPT_001", "level": 2, "description": "负责市场推广和品牌建设"},
    {"id": "DEPT_004", "name": "人力资源部", "parentId": "DEPT_001", "level": 2, "description": "负责人员招聘和员工管理"},
    {"id": "DEPT_005", "name": "财务部", "parentId": "DEPT_001", "level": 2, "description": "负责财务管理和资金运作"},
    {"id": "DEPT_006", "name": "研发一部", "parentId": "DEPT_002", "level": 3, "description": "负责核心产品研发"},
    {"id": "DEPT_007", "name": "研发二部", "parentId": "DEPT_002", "level": 3, "description": "负责创新项目研发"},
    {"id": "DEPT_008", "name": "测试部", "parentId": "DEPT_002", "level": 3, "description": "负责产品质量测试"},
    {"id": "DEPT_009", "name": "运维部", "parentId": "DEPT_002", "level": 3, "description": "负责系统运维和技术支持"},
    {"id": "DEPT_010", "name": "销售一部", "parentId": "DEPT_003", "level": 3, "description": "负责华东地区销售"},
    {"id": "DEPT_011", "name": "销售二部", "parentId": "DEPT_003", "level": 3, "description": "负责华南地区销售"},
    {"id": "DEPT_012", "name": "品牌推广部", "parentId": "DEPT_003", "level": 3, "description": "负责品牌建设和宣传"}
]

def generate_name():
    surname = random.choice(surnames)
    given_name = random.choice(given_names)
    if random.random() > 0.5:
        given_name += random.choice(given_names)
    return surname + given_name

def generate_phone(index):
    prefix = random.choice(["138", "139", "150", "151", "152", "153", "155", "156", "157", "158", "159", "180", "181", "182", "183", "185", "186", "187", "188", "189"])
    number = f"{prefix}{index:08d}"[-11:]
    return number

def generate_email(name, index):
    pinyin = ''.join([hex(ord(c))[2:] for c in name])
    return f"{pinyin}{index}@company.com"

def get_position(dept_id):
    if dept_id in ["DEPT_006", "DEPT_007"]:
        return random.choice(positions_tech)
    elif dept_id == "DEPT_008":
        return random.choice(positions_tech)
    elif dept_id == "DEPT_009":
        return random.choice(positions_tech)
    elif dept_id in ["DEPT_010", "DEPT_011", "DEPT_012"]:
        return random.choice(positions_market)
    elif dept_id == "DEPT_004":
        return random.choice(positions_hr)
    elif dept_id == "DEPT_005":
        return random.choice(positions_finance)
    else:
        return random.choice(positions_general)

def generate_employees():
    employees = []
    
    employees.append({
        "id": "EMP_001",
        "name": "张伟",
        "position": "总经理",
        "departmentId": "DEPT_001",
        "phone": "13800000001",
        "email": "zhangwei@company.com",
        "avatar": "",
        "level": 1,
        "status": "active"
    })
    
    employees.append({
        "id": "EMP_002",
        "name": "李娜",
        "position": "技术总监",
        "departmentId": "DEPT_002",
        "phone": "13800000002",
        "email": "lina@company.com",
        "avatar": "",
        "level": 2,
        "status": "active"
    })
    
    employees.append({
        "id": "EMP_003",
        "name": "王强",
        "position": "市场总监",
        "departmentId": "DEPT_003",
        "phone": "13800000003",
        "email": "wangqiang@company.com",
        "avatar": "",
        "level": 2,
        "status": "active"
    })
    
    employees.append({
        "id": "EMP_004",
        "name": "刘芳",
        "position": "人力资源总监",
        "departmentId": "DEPT_004",
        "phone": "13800000004",
        "email": "liufang@company.com",
        "avatar": "",
        "level": 2,
        "status": "active"
    })
    
    employees.append({
        "id": "EMP_005",
        "name": "陈明",
        "position": "财务总监",
        "departmentId": "DEPT_005",
        "phone": "13800000005",
        "email": "chenming@company.com",
        "avatar": "",
        "level": 2,
        "status": "active"
    })
    
    managers = [
        ("EMP_006", "赵磊", "研发一部经理", "DEPT_006"),
        ("EMP_007", "孙丽", "研发二部经理", "DEPT_007"),
        ("EMP_008", "周杰", "测试部经理", "DEPT_008"),
        ("EMP_009", "吴敏", "运维部经理", "DEPT_009"),
        ("EMP_010", "郑涛", "销售一部经理", "DEPT_010"),
        ("EMP_011", "冯雪", "销售二部经理", "DEPT_011"),
        ("EMP_012", "袁华", "品牌推广部经理", "DEPT_012")
    ]
    
    for emp_id, name, position, dept_id in managers:
        employees.append({
            "id": emp_id,
            "name": name,
            "position": position,
            "departmentId": dept_id,
            "phone": f"138000000{emp_id.split('_')[1]}",
            "email": f"{name.lower()}@company.com",
            "avatar": "",
            "level": 3,
            "status": "active"
        })
    
    dept_distribution = {
        "DEPT_006": 5000,
        "DEPT_007": 4000,
        "DEPT_008": 2000,
        "DEPT_009": 1500,
        "DEPT_010": 3000,
        "DEPT_011": 3000,
        "DEPT_012": 1500
    }
    
    emp_index = 13
    for dept_id, count in dept_distribution.items():
        for _ in range(count):
            name = generate_name()
            emp_id = f"EMP_{emp_index:05d}"
            employees.append({
                "id": emp_id,
                "name": name,
                "position": get_position(dept_id),
                "departmentId": dept_id,
                "phone": generate_phone(emp_index),
                "email": generate_email(name, emp_index),
                "avatar": "",
                "level": 4,
                "status": "active"
            })
            emp_index += 1
    
    return employees

employees = generate_employees()

data = {
    "departments": departments,
    "employees": employees
}

output_file = r"c:\Users\Work\AndroidStudioProjects\MineAndroid\board\contacts\src\main\assets\organization.json"
with open(output_file, 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print(f"成功生成 {len(employees)} 名员工的数据")
print(f"文件已保存到: {output_file}")
