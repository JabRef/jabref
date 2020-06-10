# JabRef Bibliography Management

JabRef is an open-source, cross-platform citation and reference management tool.

Stay on top of your literature: JabRef helps you to collect and organize sources, find the paper you need and discover the latest research.
![main table](http://www.jabref.org/img/jabref-mainscreen.png)

## Features

JabRef is available free of charge and is actively developed.
It supports you in every step of your research work.

### Collect

- Search across many online scientific catalogues like CiteSeer, CrossRef, Google Scholar, IEEEXplore, INSPIRE-HEP, Medline PubMed, MathSciNet, Springer, arXiv, and zbMATH
- Import options for over 15 reference formats
- Easily retrieve and link full-text articles
- Fetch complete bibliographic information based on ISBN, DOI, PubMed-ID and arXiv-ID
- Extract metadata from PDFs
- Import new references directly from the browser with one click using the [official browser extension](https://github.com/JabRef/JabRef-Browser-Extension) for [Firefox](https://addons.mozilla.org/en-US/firefox/addon/jabref/?src=external-github),  [Chrome](https://chrome.google.com/webstore/detail/jabref-browser-extension/bifehkofibaamoeaopjglfkddgkijdlh), [Edge](https://microsoftedge.microsoft.com/addons/detail/pgkajmkfgbehiomipedjhoddkejohfna) and [Vivaldi](https://chrome.google.com/webstore/detail/jabref-browser-extension/bifehkofibaamoeaopjglfkddgkijdlh)

### Organize

- Group your research into hierarchical collections and organize research items based on keywords/tags, search terms or your manual assignments
- Advanced search and filter features
- Complete and fix bibliographic data by comparing with curated online catalogues such as Google Scholar, Springer or MathSciNet
- Customizable citation key generator
- Customize and add new metadata fields or reference types
- Find and merge duplicates
- Attach related documents: 20 different kinds of documents supported out of the box, completely customizable and extendable
- Automatically rename and move associated documents according to customizable rules
- Keep track of what you read: ranking, priority, printed, quality-assured

### Cite

- Native BibTeX and Biblatex support
- Cite-as-you-write functionality for external applications such as Emacs, Kile, LyX, Texmaker, TeXstudio, Vim and WinEdt.
- Format references in one of the many thousand built-in citation styles or create your style
- Support for Word and LibreOffice/OpenOffice for inserting and formatting citations

### Share

- Many built-in export options or create your export format
- Library is saved as a simple text file and thus it is easy to share with others via Dropbox and is version-control friendly
- Work in a team: sync the contents of your library via a SQL database

## Installation

Fresh development builds are available at [builds.jabref.org](https://builds.jabref.org/master/).
The [latest stable release is available at FossHub](https://downloads.jabref.org/).

Please see our [Installation Guide](https://docs.jabref.org/installation).

## Bug Reports, Suggestions, Other Feedback

[![Donation](https://img.shields.io/badge/donate%20to-jabref-orange.svg)](https://donations.jabref.org)
[![Paypal Donate](https://img.shields.io/badge/donate-paypal-00457c.svg?logo=paypal&style=flat-square)](https://paypal.me/JabRef)

We are thankful for any bug reports or other feedback.
If you have ideas for new features you want to be included in JabRef, [tell us in our forum](http://discourse.jabref.org/c/features)!
If you need support in using JabRef, please read [the documentation](https://docs.jabref.org/) first, the [frequently asked questions (FAQ)](https://docs.jabref.org/faq) and also have a look at our [community forum](https://discourse.jabref.org/c/help).
You can use our [GitHub issue tracker](https://github.com/JabRef/jabref/issues) to file bug reports.

An explanation of donation possibilities and usage of donations is available at our [donations page](https://donations.jabref.org).

## Contributing

[![dev-docs](https://img.shields.io/badge/dev-docs-blue)](https://devdocs.jabref.org/)
[![Help Contribute to Open Source](https://www.codetriage.com/jabref/jabref/badges/users.svg)](https://www.codetriage.com/jabref/jabref)
[![Join the chat at https://gitter.im/JabRef/jabref](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/JabRef/jabref?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![OpenHub](https://www.openhub.net/p/jabref/widgets/project_thin_badge.gif)](https://www.openhub.net/p/jabref)
[![Deployment Status](https://github.com/JabRef/jabref/workflows/Deployment/badge.svg)](https://github.com/JabRef/jabref/actions?query=workflow%3ADeployment)
[![Test Status](https://github.com/JabRef/jabref/workflows/Tests/badge.svg)](https://github.com/JabRef/jabref/actions?query=workflow%3ATests)
[![codecov.io](https://codecov.io/github/JabRef/jabref/coverage.svg?branch=master)](https://codecov.io/github/JabRef/jabref?branch=master)

Want to be part of a free and open-source project that tens of thousands of scientists use every day?
Check out the ways you can contribute, below:

- Not a programmer? Help translating JabRef at [Crowdin](https://crowdin.com/project/jabref) or learn how to help at [contribute.jabref.org](https://contribute.jabref.org)
- Quick overview on the architecture needed? Look at our [high-level documentation](https://devdocs.jabref.org/getting-into-the-code/high-level-documentation)
- For details on how to contribute, have a look at our [guidelines for contributing](CONTRIBUTING.md).
- You are welcome to contribute new features. To get your code included into JabRef, just [fork](https://help.github.com/en/articles/fork-a-repo) the JabRef repository, make your changes, and create a [pull request](https://help.github.com/en/articles/about-pull-requests).
- To work on existing JabRef issues, check out our [issue tracker](https://github.com/JabRef/jabref/issues). New to open source contributing? Look for issues with the ["good first issue"](https://github.com/JabRef/jabref/labels/good%20first%20issue) label to get started.

We view pull requests as a collaborative process.
Submit a pull request early to get feedback from the team on work in progress.
We will discuss improvements with you and agree to merge them once the [developers](https://github.com/JabRef/jabref/blob/master/DEVELOPERS) approve.

If you want a step-by-step walk-through on how to set-up your workspace, please check [this guideline](https://devdocs.jabref.org/getting-into-the-code/guidelines-for-setting-up-a-local-workspace).

To compile JabRef from source, you need a Java Development Kit 14 and `JAVA_HOME` pointing to this JDK.
To run it, just execute `gradlew run`.
When you want to develop, it is necessary to generate additional sources using `gradlew generateSource`
and then generate the Eclipse `gradlew eclipse`.
For IntelliJ IDEA, just import the project via a Gradle Import by pointing at the `build.gradle`.

`gradlew test` executes all tests. We use [Github Actions](https://github.com/JabRef/jabref/actions) for executing the tests after each commit. For developing, it is sufficient to locally only run the associated test for the classes you changed. Github will report any other failure.

[JabRef]: https://www.jabref.org



The Chinese version of the documentation


JabRef 参考书目管理系统

JabRef是一个开源，跨平台的引用和参考管理工具。

JabRef可帮助您随时掌握文献资料，收集和整理资料，找到所需的论文并发现最新的研究成果。

产品特点：
JabRef是一个可在Windows，Linux和Mac OS X上运行的跨平台应用程序。它是免费的，并且正在积极开发中。 JabRef在您每一步的研究工作中都为您提供支持。

收集：
搜索许多在线的科学目录，例如CiteSeer，CrossRef，Google Scholar，IEEEXplore，INSPIRE-HEP，Medline PubMed，MathSciNet，Springer，arXiv和zbMATH
超过15种参考格式的导入选项
轻松检索和链接全文文章
根据ISBN，DOI，PubMed-ID和arXiv-ID获取完整的书目信息
从PDF提取元数据
使用适用于Firefox，Chrome，Edge和Vivaldi的官方浏览器扩展程序，单击一下即可直接从浏览器导入新引用

组织:
将您的研究分组为分层集合，并根据关键字/标签，搜索词或您的手动分配来组织研究项目
进阶搜寻和筛选功能
通过与精选的在线目录（例如Google Scholar，Springer或MathSciNet）进行比较来完成和修正书目数据
可定制的引证密钥生成器
自定义并添加新的元数据字段或引用类型
查找并合并重复项
附加相关文档：开箱即用地支持20种不同类型的文档，可完全自定义和扩展
根据可自定义的规则自动重命名和移动关联的文档
跟踪您阅读的内容：排名，优先级，印刷内容，质量保证
引用:
支持本机BibTeX和Biblatex
外部应用程序（例如Emacs，Kile，LyX，Texmaker，TeXstudio，Vim和WinEdt）的按需编写功能。
使用数千种内置引用样式之一设置引用格式或创建样式
支持Word和LibreOffice / OpenOffice以插入和格式化引文

分享:
许多内置的导出选项或创建您的导出格式
库被保存为简单的文本文件，因此可以很容易地通过Dropbox与他人共享，并且版本控制友好
团队合作：通过SQL数据库同步库的内容

安装:
可以在builds.jabref.org上获得新的开发版本。 最新的稳定版本可从FossHub获得。
Windows：JabRef提供了一个安装程序，该安装程序还将JabRef的快捷方式添加到您的开始菜单。 另请参阅我们的Windows常见问题解答
Linux：请参阅我们的安装指南。
Mac OS X：请参阅我们的Mac OS X常见问题解答。



