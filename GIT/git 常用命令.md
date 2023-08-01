## 从master上拉取一个新分支：
git branch 查看当前分支，显示为master就行了
git checkout -b xxx 根据master分支切一个xxx分支出来
git branch 查看当前分支，显示为xxx分支就可以
git push -u origin xxx 将xxx分支推到远程上，因为远程上没有这个新的xxx分支，所以要加-u。第一次将新分支提交到远程上时需要加-u

## 提交到当前开发分支：
git branch 查看当前分支
git status 修改和添加的文件是红色的
git add . 将所有的文件推到暂存区
git status 此时修改和添加的文件是绿色的
git commit -m “” 将暂存区的代码推到本地仓库
git status 此时工作目录是干净的

git pull origin master 拉取远程分支，处理冲突

git push origin xxx 将本地仓库xxx推到远程xxx，远程上有这个分支时可以不用写origin xxx

## 合并到master分支：
git checkout master 切换到master分支
git branch 查看当前分支

git pull origin master 保险起见先拉一下master分支上的代码

git merge xxx 将xxx合并到master分支
git push 将master分支代码推到远程，因为远程上有master分支，所以可以不用加origin master



## git如何撤销上一次commit操作：

git revert HEAD   撤销上一次修改（原理是检出上次修改，然后提交一个没被修改的新版本）

git revert <commit_id> 撤销历史中的某一次修改 (可以撤销  上一次的撤销，用于回到没被撤销的版本)

获取<commit_id> :  git rev-pase head 





## 重写项目历史

### git commit --amend

`git commit --amend` 命令是修复最新提交的便捷方式。它允许你将缓存的修改和之前的提交合并到一起，而不是提交一个全新的快照。它还可以用来简单地编辑上一次提交的信息而不改变快照。

很容易就忘记了缓存一个文件或者弄错了提交信息的格式。`--amend` 标记是修复这些小意外的便捷方式。

我们说过永远不要重设和其他开发者共享的提交。对于修复也是一样：永远不要修复一个已经推送到公共仓库中的提交。



## git remote

`git remote` 命令允许你创建、查看和删除和其它仓库之间的连接。远程连接更像是书签，而不是直接跳转到其他仓库的链接。它用方便记住的别名引用不那么方便记住的 URL，而不是提供其他仓库的实时连接。



```
git remote -v
```

列出你和其他仓库之间的远程连接。同时显示每个连接的 URL。

```
git remote add <name> <url>
```

创建一个新的远程仓库连接。在添加之后，你可以将 `<name>` 作为 `<url>` 便捷的别名在其他 Git 命令中使用。

```
git remote rm <name>
```

移除名为的远程仓库的连接。

```
git remote rename <old-name> <new-name>
```

将远程连接从 `<old-name>` 重命名为 `<new-name>`。



## git fetch

```
git fetch <remote>
```

拉取仓库中所有的分支。同时会从另一个仓库中下载所有需要的提交和文件。

```
git fetch <remote> <branch>
```

和上一个命令相同，但只拉取指定的分支。



#### 远程分支

远程分支和本地分支一样，只不过它们代表这些提交来自于其他人的仓库。你可以像查看本地分支一样查看远程分支，但你会处于分离 HEAD 状态（就像查看旧的提交时一样）。你可以把它们视作只读的分支。如果想要查看远程分支，只需要向 `git branch` 命令传入 `-r` 参数。远程分支拥有 remote 的前缀，所以你不会将它们和本地分支混起来。比如，下面的代码片段显示了从 origin 拉取之后，你可能想要查看的分支：

```
git branch -r
# origin/master
# origin/develop
# origin/some-feature
```



git pull = git fetch+ git merge



## rebase

**merge 和 rebase**

一般来说，merge用于公共分支的合并，rebase用于合并自己的子分支

merge 是合并的意思，rebase是复位基底的意思。
现在我们有这样的两个分支,test和master，提交如下：

```
     D---E test
    /
A---B---C---F master
```

在master执行`git merge test`然后会得到如下结果：

```
     D--------E
    /          \
A---B---C---F---G    test , master
```

在master执行`git rebase test`,然后得到如下结果：

```
 A---B---C---F---D---E` test , master
```

可以看到merge操作会生成一个新的节点，之前提交分开显示。而rebase操作不会生成新的节点，是将两个分支融合成一个线性的操作。

通过上面可以看到，想要更好的提交树，使用rebase操作会更好一点，这样可以线性的看到每一次提交，并且没有增加提交节点。
在操作中。merge操作遇到冲突时候，当前merge不能继续下去。手动修改冲突内容后，add 修改，commit 就可以了
而rebase操作的话，会中断rebase，同时会提示去解决冲突。解决冲突后，将修改add后执行git rebase -continue继续操作，或者git rebase -skip忽略冲突。

## 强制将远程上代码覆盖本地：

git fetch –all 

git reset –hard origin/master 

git pull

## 创建分支并推送到远程仓库：

本地创建分支：git branch 分支名`````````````````````````````````````````````````````````

删除本地创建分支：git branch -d 分支名

本地创建分支并切换到该分支：git checkout -b 分支名   

分支推送到远程仓库：git push origin 分支名

删除远程分支：git push origin –delete 分支名


