# huixuan-mall

基于 Spring Cloud Alibaba 的微服务电商系统，覆盖商品、购物车、用户、交易、支付等核心链路，支持商品搜索、购物车管理、下单、支付回调、库存扣减与分布式事务。

## 技术栈

| 类别 | 技术 |
| --- | --- |
| 运行环境 | Java 17 |
| 基础框架 | Spring Boot 4.0.8 |
| 微服务 | Spring Cloud 2025.1.x、Spring Cloud Alibaba 2025.1.0.0 |
| 注册 / 配置中心 | Nacos |
| 网关 | Spring Cloud Gateway（动态路由、JWT 鉴权） |
| 服务调用 | OpenFeign |
| 数据访问 | MyBatis-Plus 3.5.17、MySQL 8 |
| 缓存 | Redis 7 |
| 消息队列 | RabbitMQ 4（延迟消息插件） |
| 分布式事务 | Seata 2.5（AT 模式） |
| 搜索引擎 | Elasticsearch 9 + IK 分词 |
| 接口文档 | Knife4j（SpringDoc） |

## 模块

| 模块 | 说明 |
| --- | --- |
| `hm-common` | 公共模块：统一异常与响应、分页、工具类、用户上下文、MyBatis / MQ 自动配置 |
| `hm-api` | 服务间 Feign 客户端与共享 DTO / PO |
| `hm-gateway` | 统一入口：动态路由、JWT 鉴权、登录校验 |
| `item-service` | 商品管理与商品搜索（Elasticsearch） |
| `cart-service` | 购物车 |
| `user-service` | 用户、收货地址、登录 |
| `trade-service` | 订单、库存扣减、订单超时关闭 |
| `pay-service` | 支付单、支付回调 |

## 功能

- **商品**：分页管理、上下架、批量查询、库存扣减与恢复
- **搜索**：关键字分词匹配、分类 / 品牌过滤、价格区间、销量 / 价格排序、分页、过滤项聚合
- **购物车**：加购、修改数量、删除、下单后清理、数量上限校验
- **订单**：创建订单、库存校验与扣减、超时未支付自动关单（RabbitMQ 延迟消息）
- **支付**：支付单创建、支付回调、支付状态通知
- **分布式事务**：Seata AT 模式保证下单链路一致性

## 目录结构

```
huixuan-mall
├── hm-common          # 公共依赖与自动配置
├── hm-api             # Feign 客户端与共享模型
├── hm-gateway         # 网关
├── item-service       # 商品 / 搜索服务
├── cart-service       # 购物车服务
├── user-service       # 用户服务
├── trade-service      # 交易服务
└── pay-service        # 支付服务
```

## 商品搜索

商品数据存放在 Elasticsearch 的 `items` 索引中：`name` 使用 `ik_max_word` 分词，`brand`、`category` 为 `keyword`，`price`、`sold`、`commentCount` 为数值类型。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/search/list` | 商品搜索与分页 |
| POST | `/search/filters` | 搜索过滤项（分类、品牌） |

`/search/list` 参数：

| 参数 | 说明 |
| --- | --- |
| `key` | 关键字，按商品名称分词匹配 |
| `category` / `brand` | 分类 / 品牌精确过滤 |
| `minPrice` / `maxPrice` | 价格区间（单位：分） |
| `sortBy` / `isAsc` | 支持 `price`、`sold`；不传时按相关度排序 |
| `pageNo` / `pageSize` | 分页参数 |

示例：

```bash
curl "http://localhost:8080/search/list?key=手机&pageNo=1&pageSize=20&sortBy=price&isAsc=false"

curl -X POST "http://localhost:8080/search/filters" \
  -H "Content-Type: application/json" \
  -d '{"key":"手机","minPrice":0,"maxPrice":999999}'
```

`/search/list` 返回 `{ total, pages, list }`；`/search/filters` 返回 `{ "category": [...], "brand": [...] }`。

## 环境准备

| 服务 | 默认地址 |
| --- | --- |
| MySQL | `192.168.100.128:3307` |
| Redis | `192.168.100.128:6379` |
| RabbitMQ | `192.168.100.128:5672` |
| Nacos | `192.168.100.128:8848` |
| Seata | `192.168.100.128:8091` |
| Elasticsearch | `192.168.100.128:9200` |

各服务通过 Nacos 读取公共配置：`shared-jdbc.yaml`、`shared-log.yaml`、`shared-swagger.yaml`、`shared-seata.yaml`、`shared-rabbitmq.yaml`。

## 本地运行

```bash
mvn clean install -DskipTests
```

随后依次启动 `hm-gateway`、`user-service`、`item-service`、`cart-service`、`trade-service`、`pay-service`（IDE 中直接运行各 `*Application`，或 `mvn -pl <module> spring-boot:run`）。

默认 profile 为 `dev`；本地调试可追加 `--spring.profiles.active=local`。`application-local.yaml` 含本机地址与明文密码，默认不纳入版本管理。

## 服务端口

| 服务 | 端口 |
| --- | --- |
| hm-gateway | 8080 |
| item-service | 8081 |
| cart-service | 8082 |
| user-service | 8084 |
| trade-service | 8085 |
| pay-service | 8086 |

## 配置说明

| 配置项 | 说明 |
| --- | --- |
| `hm.db.*` | 数据源地址、库名、账号密码 |
| `hm.es.addr` | Elasticsearch 地址 |
| `hm.jwt.*` | JWT 密钥文件、别名、密码与有效期 |
| `hm.swagger.package` | 接口文档扫描包 |

## 接口文档

服务启动后访问 `http://localhost:8081/doc.html`（端口按服务不同），即可查看 Knife4j 接口文档。

## 前端

前端为静态页面，由 Nginx 托管，`/api` 反向代理到网关 `8080`。
