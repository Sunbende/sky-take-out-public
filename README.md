# sky-take-out 苍穹外卖

基于 Spring Boot 的苍穹外卖后端系统，提供商家管理端（Admin）和用户端（User）两套 RESTful API，支持菜品/套餐管理、订单处理、微信支付、营业数据报表、WebSocket 实时通知等完整的外卖业务功能。

## 一、技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 核心框架 | Spring Boot | 2.7.3 |
| 持久层 | MyBatis + PageHelper | mybatis-spring 2.2.0 / pagehelper 1.3.0 |
| 数据库 | MySQL + Druid 连接池 | druid 1.2.1 |
| 缓存 | Redis (Spring Data Redis) | - |
| 接口文档 | Knife4j | 3.0.2 |
| 认证 | JWT (jjwt) | 0.9.1 |
| 对象存储 | 阿里云 OSS SDK | 3.10.2 |
| 支付 | 微信支付 V3 (wechatpay-apache-httpclient) | 0.4.8 |
| 报表导出 | Apache POI | 3.16 |
| 实时通信 | WebSocket | spring-boot-starter-websocket |
| AOP | AspectJ | 1.9.4 |
| 工具 | Lombok、Fastjson、commons-lang | - |

## 二、项目结构

```
sky-take-out/
├── sky-common          # 公共模块：常量、异常、工具类、属性配置、统一返回结果
│   ├── constant        # 业务常量（消息、密码、状态、JWT claims、自动填充字段名）
│   ├── context         # BaseContext 线程上下文
│   ├── enumeration     # OperationType 枚举（INSERT/UPDATE）
│   ├── exception       # 业务异常体系（BaseException 及其子类）
│   ├── json            # JacksonObjectMapper 自定义序列化
│   ├── properties      # 配置属性类（JwtProperties、AliOssProperties、WeChatProperties）
│   ├── result          # Result、PageResult 统一响应封装
│   └── utils           # JwtUtil、HttpClientUtil、AliOssUtil、WeChatPayUtil
├── sky-pojo            # 数据模型模块
│   ├── entity          # 数据库实体（Employee、Dish、Setmeal、Orders 等）
│   ├── dto             # 数据传输对象（请求参数）
│   └── vo              # 视图对象（响应数据）
└── sky-server          # 服务模块（启动模块）
    ├── annotation      # @AutoFill 自动填充注解
    ├── aspect          # AutoFillAspect 切面（自动填充创建/更新人与时间）
    ├── config          # OssConfiguration、RedisConfiguration、WebMvcConfiguration、WebSocketConfiguration
    ├── controller      # 控制器
    │   ├── admin       # 管理端接口
    │   ├── user        # 用户端接口
    │   └── notify      # 支付回调接口
    ├── handler         # GlobalExceptionHandler 全局异常处理
    ├── interceptor     # JwtTokenAdminInterceptor、JwtTokenUserInterceptor
    ├── mapper          # MyBatis Mapper 接口
    ├── service         # 业务接口与实现
    ├── task            # OrderTask、WebSocketTask 定时任务
    ├── websocket       # WebSocketServer
    └── resources
        ├── mapper      # MyBatis XML 映射文件
        ├── template    # 运营数据报表 Excel 模板
        ├── application.yml
        └── application-dev.yml
```

## 三、核心功能模块

### 1. 员工管理（admin/employee）
- 员工登录（JWT 鉴权）、退出登录
- 新增员工、分页查询、启用/停用账号
- 按 ID 查询、编辑员工信息
- 修改密码

### 2. 分类管理（admin/category）
- 新增分类、分页查询、删除、修改分类
- 启用/禁用分类
- 按类型（菜品/套餐）查询分类

### 3. 菜品管理（admin/dish）
- 新增菜品（含口味 DishFlavor）
- 分页查询、批量删除、按 ID 查询菜品及口味
- 修改菜品、更新售卖状态
- 按分类查询菜品

### 4. 套餐管理（admin/setmeal）
- 新增套餐（含套餐菜品关联 SetmealDish）
- 分页查询、批量删除、按 ID 查询套餐及菜品
- 修改套餐、更新售卖状态
- 按分类查询套餐

### 5. 订单管理
**管理端（admin/order）**
- 订单条件搜索、订单状态统计
- 订单详情、接单、拒单、取消、派送、完成订单

**用户端（user/order）**
- 用户下单、微信支付、历史订单查询
- 订单详情、取消订单、再来一单、客户催单

### 6. 营业数据报表（admin/report）
- 营业额统计（TurnoverReportVO）
- 用户统计（UserReportVO）
- 订单统计（OrderReportVO）
- 销量 Top10 统计（SalesTop10ReportVO）
- 导出运营数据 Excel 报表

### 7. 工作台（admin/workspace）
- 今日营业数据、订单总览
- 菜品总览、套餐总览

### 8. 店铺状态（shop）
- 管理端设置店铺营业状态
- 用户端查询店铺状态（Redis 缓存）

### 9. 用户端功能
- **用户登录**：微信授权登录（user/user）
- **购物车**（user/shoppingCart）：添加、查询、清空
- **地址簿**（user/addressBook）：增删改查、设置默认地址
- **菜品浏览**（user/dish）：按分类查询（Redis 缓存）
- **套餐浏览**（user/setmeal）：按分类查询（Spring Cache 注解缓存）

### 10. 支付回调（notify）
- `/notify/paySuccess`：微信支付成功回调，校验并更新订单状态
- `/notify/refundSuccess`：退款回调

## 四、关键设计

### 1. 统一响应
`Result<T>` 封装统一返回结构（code / msg / data），`PageResult` 封装分页结果（total / records）。

### 2. 全局异常处理
`GlobalExceptionHandler` 使用 `@RestControllerAdvice` 捕获 `BaseException` 业务异常与 SQL 唯一键异常，返回统一 `Result`。

### 3. 自动填充
`@AutoFill(OperationType.INSERT/UPDATE)` 注解 + `AutoFillAspect` 切面，自动填充 `createTime/updateTime/createUser/updateUser` 字段，无需在业务代码中手动赋值。

### 4. JWT 双端鉴权
- 管理端：`JwtTokenAdminInterceptor` 解析 `token` 请求头，提取 `empId` 写入 `BaseContext`
- 用户端：`JwtTokenUserInterceptor` 解析 `authentication` 请求头，提取 `userId` 写入 `BaseContext`
- 通过 `BaseContext`（ThreadLocal）在同线程内传递当前登录用户 ID

### 5. 缓存策略
- Redis 缓存：店铺状态、用户端菜品列表
- Spring Cache 注解：用户端套餐列表（`@Cacheable`）

### 6. 定时任务
- `OrderTask`：定时处理超时未支付订单（自动取消）、长时间派送中订单（自动完成）
- `WebSocketTask`：定时向客户端推送心跳消息

### 7. WebSocket 实时通知
`WebSocketServer` 管理客户端连接，订单状态变更时向管理端推送实时通知（如客户催单）。

## 五、数据库

数据库名：`sky_take_out`，主要表：

| 表名 | 说明 |
|------|------|
| employee | 员工 |
| category | 分类（菜品/套餐） |
| dish | 菜品 |
| dish_flavor | 菜品口味 |
| setmeal | 套餐 |
| setmeal_dish | 套餐-菜品关联 |
| user | C 端用户 |
| address_book | 地址簿 |
| shopping_cart | 购物车 |
| orders | 订单 |
| order_detail | 订单明细 |

## 六、环境配置

配置文件：`sky-server/src/main/resources/application-dev.yml`

```yaml
sky:
  datasource:
    host: localhost
    port: 3306
    database: sky_take_out
    username: root
    password: <your_password>
  redis:
    host: localhost
    port: 6379
    password: <your_password>
    database: 10
  alioss:
    endpoint: oss-cn-beijing.aliyuncs.com
    access-key-id: <your_access_key_id>
    access-key-secret: <your_access_key_secret>
    bucket-name: <your_bucket_name>
  wechat:
    appid: <your_appid>
    secret: <your_secret>
    mchid: <your_mchid>
    # ... 其他微信支付配置
  shop:
    address: <shop_address>
  baidu:
    ak: <your_baidu_ak>
```

> 注意：`application-dev.yml` 中包含数据库、Redis、阿里云 OSS、微信支付等敏感配置，生产环境请使用环境变量或配置中心管理，切勿提交到代码仓库。

## 七、启动方式

### 1. 后端服务

**前置条件**：JDK 8+、Maven、MySQL、Redis

1. 创建数据库 `sky_take_out` 并导入建表脚本
2. 修改 `application-dev.yml` 中的数据库、Redis、OSS、微信支付等配置
3. 设置 `SKY_JWT_ADMIN_SECRET` 和 `SKY_JWT_USER_SECRET` 环境变量，分别提供管理端和用户端的 JWT 签名密钥
4. 启动 MySQL 与 Redis 服务
5. 运行启动类 `SkyApplication`
6. 服务默认端口：`8080`
7. 接口文档地址：`http://localhost:8080/doc.html`（Knife4j）

### 2. 前端服务

本项目提供两个前端压缩包，分别对应管理端和用户端：

#### （1）管理端前端（nginx-1.20.2.zip）

管理端前端基于 Nginx 部署，负责商家后台的菜品/套餐/订单/员工等管理业务。

**Nginx 端口分工**（来自 `conf/nginx.conf`）：

| 角色 | 端口 | 说明 |
|------|------|------|
| Nginx（前端静态资源 + 反向代理） | `80` | 浏览器访问入口 |
| 后端 Spring Boot（内嵌 Tomcat） | `8080` | Nginx 通过 `proxy_pass` 转发到此端口 |

Nginx 配置的反向代理规则：

- `/api/` → `http://localhost:8080/admin/`（管理端接口）
- `/user/` → `http://127.0.0.1:8080/user/`（用户端接口）
- `/ws/` → `http://127.0.0.1:8080/ws/`（WebSocket）

> 说明：Nginx 监听 **80** 端口，后端监听 **8080** 端口，两者分工明确，**不存在端口冲突**，可同时运行。

**启动步骤**：

1. 解压 `nginx-1.20.2.zip`
2. 进入解压后的目录，双击运行 `nginx.exe`（或在该目录下执行 `start nginx`）
3. 在浏览器访问 `http://localhost` 即可进行调试（Nginx 监听 80 端口，无需加端口号）

> 提示：管理端前端请求的接口会通过 Nginx 反向代理到后端 `8080` 端口，因此必须先启动后端服务，否则管理端页面数据无法加载。

#### （2）用户端小程序（mp-weixin.zip）

用户端为微信小程序，负责 C 端用户的浏览菜品、下单、支付、地址管理等业务。

**启动步骤**：

1. 解压 `mp-weixin.zip`
2. 下载并安装[微信开发者工具](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html)
3. 打开微信开发者工具，选择「导入项目」，目录指向解压后的小程序根目录
4. 填写 AppID（可使用测试号），点击「导入」
5. 在开发者工具中点击「编译」即可预览和调试

> 小程序中调用的后端接口地址需与本地后端服务地址保持一致，如接口请求域名/IP 与端口不匹配，请在小程序全局配置中修改。

## 八、订单状态流转

```
待付款(1) ──支付成功──> 待接单(2) ──接单──> 待派送(3) ──派送──> 派送中(4) ──完成──> 已完成(5)
    │                     │                    │
    └──超时取消──> 已取消(6)  └──拒单──> 已取消(6)   └──取消──> 已取消(6)
```
