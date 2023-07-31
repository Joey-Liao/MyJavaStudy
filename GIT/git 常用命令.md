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

git poll 拉取远程分支，处理冲突

git push origin xxx 将本地仓库xxx推到远程xxx，远程上有这个分支时可以不用写origin xxx

## 合并到master分支：
git checkout master 切换到master分支
git branch 查看当前分支

git pull origin master 保险起见先拉一下master分支上的代码

git merge xxx 将xxx合并到master分支
git push 将master分支代码推到远程，因为远程上有master分支，所以可以不用加origin master

## git如何撤销上一次commit操作：

第一种情况，如果还没有push，只是在本地commit：git reset –hard <commit_id>

第二种情况，如果已经push：git revert <commit_id>

 获取<commit_id> :  git rev-pase head 

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