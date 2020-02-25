# dreamwork-sshd

### 介绍
使用 apache sshd 框架，提供一个可用的 sshd 服务器端半成品。快速开发基于 ssh 的 CLI应用程序。

## 软件架构
- 底层采用 `apache sshd` 框架来提供 `SSH` 网络服务。
- 内置一个实现了
  * RFC 318 (TELNET), 
  * RFC 854 – RFC 861 (TELNET sub-options)
  * RFC 1073 (TELNET Window Size)
  
  telnet协议栈的 java 实现
- 扩展 `org.dreamwork.telnet.command.Command` 以快速开发命令来实现功能

## 使用说明

### 最简单的例子：
```java
package org.dreamwork.sshd.example;

import org.dreamwork.config.IConfiguration;
import org.dreamwork.config.PropertyConfiguration;
import org.dreamwork.network.sshd.Sshd;

import java.util.Properties;

public class BasicSshdApplication {
    public static void main (String[] args) throws Exception {
        IConfiguration conf = new PropertyConfiguration (new Properties ());

        Sshd sshd = new Sshd (conf);
        sshd.init (null);
        sshd.bind ();
    }
}
```
然后通过命令 
```shell script
ssh -p 50022 root@127.0.0.1
```
来登录服务，root的默认密码是`123456`。您可以在登录到sshd服务后使用`passwd`命令来修改root的密码

### 配置 sshd 服务
- sshd 服务默认监听 `50022` 端口
- ca 文件的默认存储位置是 ${user.home}/.ssh-server/known-hosts
- sqlite 数据库文件的默认存储位置是 ${user.home}/.ssh-server/database.db

您可以通过配置来修改这些参数
- 键值 `service.sshd.port` 用来修改监听端口
- 键值 `service.sshd.cert.file` 用来修改 ca 文件的存放路径
- 键值 `database.file` 用来修改 sqlite 数据库文件的存放路径
- 键值 `default.root.password` 用来修改默认的root密码

以上所有键值都可通过添加系统属性来覆盖默认值

### 内置数据库
`dreamwork sshd` 采用数据库来存储 用户数据 及 系统配置参数

`org.dreamwork.network.sshd.Sshd` 通过 `init (IDatabase)` 方法传入一个数据库对象，当该方法传参为 `null` 时，内部将创建一个 sqlite3 文件数据库。当您希望 sshd-server 和您的应用共用一个数据库时，`init`方法的参数必须是一个 **非空** 的数据库对象。

## 命令
命令为`sshd-server`提供一组特定功能的人机接口，通过在 `org.dreamwork.network.sshd.Sshd`上注册 `org.dreamwork.telnet.command.Command`的实现类来提供服务。
### 内置命令
在上面的基础实现中，`sshd-server` 内置了一组命令，您可以登陆服务后通过命令 `help`来查看：
```shell script
console> h
clear            clear the screen and home cursor
echo             shows something
env              show environment
exit             alias for command 'quit'
help        h    show this help list
history          show history
passwd           change current or spec user's password
quit        q    exit
set              setting console env
shutdown         shutdown the sshd server
unset            unset a console env
user             user management
```

### 扩展自定义命令
通过扩展 `org.dreamwork.telnet.command.Command`抽象类来自定义命令。

#### 一个简单的自定义命令
我们来实现一个简单的命令：`datetime`，来实现显示当前时间
```java
package org.dreamwork.sshd.example;

import org.dreamwork.telnet.Console;
import org.dreamwork.telnet.command.Command;

import java.io.IOException;
import java.text.SimpleDateFormat;

public class DateTimeCommand extends Command {
    private SimpleDateFormat sdf = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
    public DateTimeCommand () {
        super ("datetime", null, "show current date time");
    }

    /**
     * 执行命令
     *
     * @param console 当前控制台
     * @throws IOException io exception
     */
    @Override
    public void perform (Console console) throws IOException {
        console.println (sdf.format (System.currentTimeMillis ()));
    }
}
```
- **构造函数**：原型为 `Command (String name, String alias, String description)`，其中：
	- **`name`**：指定命令的名称，以后在控制台输入的主命令就是这个
	- **`alias`**：命令的别名，可以为命令提供一个简短的别名
	- **`description`**：命令的简短描述，内置命令`help`将使用这个描述
- **命令处理函数**：签名为`void perform (Console console) throws IOException`，其中：
	- **`console`**：即控制台对象，可以打印命令处理结果

现在，我们可以将这个命令注册到`sshd-server`中了
```java
public class BasicSshdApplication {
    public static void main (String[] args) throws Exception {
        IConfiguration conf = new PropertyConfiguration (new Properties ());

        Sshd sshd = new Sshd (conf);
        sshd.init (null);

        // 注册 DateTimeCommand
        sshd.registerCommands (new DateTimeCommand ());

        sshd.bind ();
    }
}
```
再次启动程序，并使用 ssh 命令登录到付，键入 help 命令，查看
```shell script
console> h
clear            clear the screen and home cursor
datetime         show current date time
echo             shows something
env              show environment
exit             alias for command 'quit'
help        h    show this help list
history          show history
passwd           change current or spec user's password
quit        q    exit
set              setting console env
shutdown         shutdown the sshd server
unset            unset a console env
user             user management
```
多了一个命令
```text
datetime         show current date time
```
执行命令
```shell script
console> datetime
2020-02-25 19:37:41
console>
```

## 深入`sshd-server`及`Command`
更高级/深入的内容请参见 "[sshd-server 使用手册](https://github.com)"

## 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request