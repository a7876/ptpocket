## PtPocket 内存缓存数据库实现

学习Redis的设计原理，基于Java实现一个简易的内存数据库，网络请求部分基于Netty4

在笔者电脑上测试，可以在全局10万键的情况下，内部统计每秒处理命令峰值达14万

目前实现了哈希，有序集合数据结构（基于跳表）

实现了基本的追加命令持久化功能，服务器接受命令之后持久化命令，并在每次启动时从磁盘装载数据

### 命令类型

**通用命令**

```
DEL key  删除全局哈希表的一个键值对

EXPIRE、EXPIRE_MILL key 以秒、毫秒设置某个全局键的相对过期时间

SELECT num  选择一个数据库

PERSIST key 取消一个全局键的过期时间

STOP 停止服务器

INFO 获取一些服务器的信息
```

**全局哈希命令**

```
GET key 获取某个key对应的值

SET key value 设置键值对
```

**内嵌哈希命令**

```
HSET key innerKey value 向某个内嵌哈希表中插入一对键值对

HGET key innerKey 从某个内嵌哈希表中获取一个值

HDEL key innerKey 从某个内嵌哈希表中删除一个键值对
```

**有序集合命令（默认内部以score升序排序）**

```
ZADD key score value 向某个有序集合中添加一个对象及其的double类型分值

ZDEL key value 删除某个有序集合中的一个值及其分值

ZRANGE key int int 从有序集合中查找从第一个int到第二个int之间的值

ZREVERRANGE key int int 从有序集合中逆序查找从第一个int到第二个int之间的值

ZRANK key value 从有序集合中查找某个值的排名

ZREVERSERANK key value 从有序集合中反向查找某个值的排名

ZRANGESCORE key double doube 从有序集合中返回分值在第一个double到第二个double的值

ZSCORE key value 从有序集合中返回对应值的score
```

### 启动方式

项目分为三个module，server为服务器，common为公用协议数据，client为服务端

**Server**

server module的core包下Main即服务器主入口，启动主方法即可启动服务器

其中默认配置文件是在server resource目录下的 `ptpocket.properties`

启动时可以在**第一个**启动参数指定配置文件

```shell
java ... xxx.properties
```

**Client**

client module的util包下有PocketTemplate作为命令接口实现

```java
class Example {
    void example() {
        Client c = new Client(ip, port);
        PocketTemplate pt = new PocketTemplate(c, ObjectEncode, ObjectDecode, defaultDb);
    }
}
```

具体见client module下的test文件夹中的相关测试类

### 模块构成

```
|—— server
|   |——core
|   |   |—— Main 主启动类
|   |   |—— ServerConfiguration 配置类
|   |   |—— ...
|   |——datastructrue
|   |   |——Hash 哈希表实现（支持渐进rehash）
|   |   |——SortedSet 有序集合实现（基于跳表和哈希表）
|   |   |——...
|   |——entity 包含命令和响应的相关封装对象
|   |——log 日志接口
|   |——persistence
|   |   |——appendfile 
|   |   |   |——AppendFilePersistence 追加命令持久化实现类
|   |   |   |——...

|—— client
|   |——core
|   |   |——Client 客户端连接封装类
|   |   |——PocketOperation 底层命令操作接口实现
|   |   |——...
|   |——entity 包含数据封装对象
|   |——exception 异常定义包
|   |——util
|   |   |——PocketTemplate 高层命令接口实现
|   |   |——ObjectDecoder 对象解码接口
|   |   |——ObjectEncoder 对象编码接口

|——common
|   |——common
|   |   |——Protocol 通信协议定义
|   |   |——CommandType 命令定义
|   |   |——ResponseType 响应定义


此外server和client两个module下都有test测试目录和一些测试类
```