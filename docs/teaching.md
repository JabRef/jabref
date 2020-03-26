# JabRef as Basis for Teaching Material

JabRef can be used as training object for software engineering.

As instructor, please follow this check list:

1. Be aware that JabRef is run by volunteers. This implies that the development team cannot ensure to provide feedback on code within hours.
2. Be aware that from the first pull request to the final acceptance the typical time needed is two weeks.
3. Be aware that JabRef tries to achieve high code quality. This leads to code reviews requiring actions from the contributors. This also applies for code of students. Read on at our [Development Strategy](development-strategy.md) for more details.
4. Choose tasks from one of the folowing boards. Write a comment to each issue so that it can be reserved to your course.
   - Feature Board: <https://github.com/JabRef/jabref/projects/7>
   - Bug Board: <https://github.com/JabRef/jabref/projects/5>
5. Be aware that the difficuly of bugs and feature vary. A task should be chosen with care. The JabRef team can help here: The majority of the team has experiences in teaching
6. Get in touch with the JabRef team to discuss details. We offer email, skype, [gitter.im](https://gitter.im/JabRef/jabref), discort. Get in touch with [@koppor](https://github.com/koppor/) to find the right channel and to start forming the success of your course.

For a near-to-perfect preparation and effect of the course, we ask you to get in touch with us **four weeks** in advance. Then, the JabRef team can a) learn about the starting skill level of the students, b) the aimed skill level at the end of the course, c) the amount of time the students are given to learn about and contribute to JabRef, d) check the [feature board](https://github.com/JabRef/jabref/projects/7) for appropriate tasks (and fill it as needed), e) recommend appropriate features.

It is also possible to just direct students to our [Contribution Guide](https://github.com/JabRef/jabref/blob/master/CONTRIBUTING.md#contributing-guide). The learning effect may be lower as time of the students has to be spend to a) learn about JabRef and b) select an appropriate issue.

Since a huge fraction of software costs is spend on [software maintenance](https://en.wikipedia.org/wiki/Software_maintenance), adding new features also educates in that aspect: perfective maintenance<sup id="a1">[1](#LientzSwanson)</sup> is trained. When fixing bugs, corrective maintenance<sup>[1](#LientzSwanson)</sup> is trained.

## Process for contributions

There is no special process for student contributions. We want to discuss it nevertheless to increase awareness of the time required from starting the contribution until the inclusion in a release of JabRef.

The process for accepting contributions is as below.
The syntax is [BPMN](https://en.wikipedia.org/wiki/Business_Process_Model_and_Notation) modeled using [bpmn.io](https://bpmn.io/).

[![process](images/contribution-process-reviews.svg)](images/contribution-process-reviews.svg)

In short, the contribution is **reviewed by two JabRef developers**.
Typically, they have constructive feedback on the contribution.
This means, that the contributors get comments on their contriubiton enabling them to level-up their coding skill.
Incorporating the improvments takes time, too.
The benifit is two fold: a) contributors improve their coding skills and b) JabRef's code quality improves.
All in all, we ask to respect the aims of the JabRef team and to reserve time to incorporate the reviewer's comemnts

## Process for Java newcomers

Newcomers are invited to follow the process described above.
In case the capacity of the instructing university allows, we propose a three step approach.
First the contributors prepare their contribution as usual.
Then, they submit the pull request *to a separate repository*.
There, the instructor reviews the pull request and provides feedback.
This happens in a loop until the instructor shows the green light.
Then, the pull request can be submitted to the main JabRef repository.
This will help to reduce the load on the JabRef team and improve the quality of the initial pull request.

[![process with instructor](images/contribution-process-reviews-with-instructor.svg)](images/contribution-process-reviews-with-instructor.svg)

## Past courses

> In case your course is missing, feel free to add it.

### English

#### Harbin Institute of Technology (HIT), China

Course: Open Source Software Development

- Summary: In this course, students will be introduced to the processes and tools specific to Open Source Software development, and they will analyze existing projects to understand the architecture and processes of these projects. In addition, students will attempt to contribute source code to a large existing Open Source Software project.
- Course offered in 2018 and 2019. Examples of merged pull requests: [4217](https://github.com/JabRef/jabref/pull/4217), [4255](https://github.com/JabRef/jabref/pull/4255), [4227](https://github.com/JabRef/jabref/pull/4227), [4260](https://github.com/JabRef/jabref/pull/4260), [5150](https://github.com/JabRef/jabref/pull/5150)

#### Northern Arizona University (NAU), USA

Course [CS499 - Open Source Software Development](https://github.com/igorsteinmacher/CS499-OSS)

- Summary: Students experience the process of getting involved in an Open Source project by engaging with a real project. Their goal is to make a "substantial" contribution to a project. 
- Course offered in 2018

### German

#### Universität Basel, Switzerland

Course [10915-01: Software Engineering](https://dmi.unibas.ch/de/studium/computer-science-informatik/lehrangebot-hs18/vorlesung-software-engineering/)

- Lecture Materials: <https://github.com/unibas-marcelluethi/software-engineering>
- Successfully run Q3 2019.
- Excercise touching JabRef:
  - General idea: identify a feature missing in JabRef and develop specification, system design, and implementiation of the feature.
  - Introduction to JabRef's code: [Exercise 5](https://github.com/unibas-marcelluethi/software-engineering/blob/master/docs/week5/exercises/practical-exercises.md): Introduction into JabRef code.
  - Prominent feature implenented: Parse full text references using Grobid. PR [#5614](https://github.com/JabRef/jabref/pull/5614).

#### University of Stuttgart, Germany

Course "Softwarepraktikum" as part of the [BSc Informatik](https://www.f05.uni-stuttgart.de/informatik/interessierte/bachelor/informatik/)

- Summary: A group of three students experienced the full software engineering process within one semester. They worked part-time for the project.
- Successfully run in 2012

Course [Studienprojekt](https://www.f05.uni-stuttgart.de/informatik/studierende/bachelor/stupro/) as part of the [BSc Software Engineering](https://www.uni-stuttgart.de/en/study/study-programs/Software-Engineering-B.Sc-00001./)

- Summary: A group of nine students experienced the full software engineering process within one year. They worked part-time for the project.
- Successfully run in 2015/2016

Course "Programming and Software Development" as part of the [BSc Software Engineering](https://www.uni-stuttgart.de/en/study/study-programs/Software-Engineering-B.Sc-00001./)

- Summary: One excersice to contribute a minor fix or feature to JabRef. Goal: learn contribution to an open source project using git and GitHub.
- Successfuly run in 2018

### Swedish

#### KTH Royal Institute of Technology, Sweden

Course [DD2480 Software Engineering Fundamentals](https://www.kth.se/student/kurser/kurs/DD2480?l=en)

- Summary: Groups of students from three to five persons experienced the whole software engineering process within a week: From the requirements specification to the final pull request.
- Successfuly run in 2020

### Portuguese

#### Federal University of Technology, Paraná, Brazil

Course [Open Source Software](https://github.com/igorsteinmacher/DSL-UTFPR)

- Summary: Students are requested to contribute to an Open Source project to learn about maintenance and evolution of software projects. This project is the predecessor of NAU's CS499.
- Course offered from 2013 to 2016 with different names

## References

<a id="LientzSwanson" href="#a1">1</a>: Lientz B., Swanson E., 1980: Software Maintenance Management. Addison Wesley, Reading, MA.

<!-- markdownlint-disable-file MD033 -->
