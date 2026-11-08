[![ChatEx Logo](https://github.com/TheJeterLP/ChatEx/blob/master/logo.png?raw=true)](https://www.spigotmc.org/resources/chatex-continued.71041/)
[![Create DevBuild](https://github.com/TheJeterLP/ChatEx/actions/workflows/autobuild.yml/badge.svg)](https://github.com/TheJeterLP/ChatEx/actions/workflows/autobuild.yml)
[![JitPack](https://jitpack.io/v/TheJeterLP/ChatEx.svg)](https://jitpack.io/#TheJeterLP/ChatEx)
================================
Recode of the original ChatManager from PermissionsEx, which is no longer supported.
This is the only official version.

If you need to report a bug or want to suggest a new feature, please use GitHubs Issue trackl.

If you want to use ChatEx as a Maven dependency, just click on the Jitpack image above
or [Here](https://jitpack.io/#TheJeterLP/ChatEx) to read more about how to add it as a dependency. You can use the
versions from github releases

[![BStats](https://bstats.org/signatures/bukkit/ChatEx.svg)](https://bstats.org/plugin/bukkit/ChatEx/7744)


Links: 
- [SpigotMC](https://www.spigotmc.org/resources/chatex.71041/)
- [Hangar (PaperMC)](https://hangar.papermc.io/TheJeterLP/ChatEx)
- [Modrinth](https://modrinth.com/project/chatex)

## Simple Voice Chat group chat

On Paper servers with Simple Voice Chat installed, ChatEx adds `#message` and `/gc message`
for messages sent only to online players connected to the same current voice-chat group.
The feature is optional: ChatEx's local and global chats continue to work if Simple Voice Chat
is not installed. Use `/chatex reload` to apply changes to the generated `group-chat` config
section. The `chatex.groupchat.use` permission defaults to `true`.

Build the installable JAR with `mvn package` or `gradle shadowJar`, then install
`target/ChatEx.jar` (Maven) or `build/libs/ChatEx-3.3.0.jar` (Gradle) in the server's `plugins`
directory, replacing the previous ChatEx JAR after backing up the server configuration. Install
Simple Voice Chat separately; its API is compile-only and is not included in the ChatEx JAR.
Run `/chatex reload` after changing the generated config section; restart the server after
installing or updating the plugin.
