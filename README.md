# The School App

## Overview

This repository contains the code for The School App I developed in a team working together with [Christina Shiamma](https://github.com/cshiam02) and [Tien-Yu Lo](https://github.com/yoyo27429) in May 2020 while studying at the University of Southampton. The aim of this project was to develop a mobile application to provide a platform for simple and convenient communication between a school, its students and their parents. The goal was to develop a native Android application, which provides an interactive environment, enabling the school administrator to provide updates on various events and activities to both the students and their parents.

## Features

The School App consists of Student, Parent, and Admin sections. The Student section consists of signup, login, viewing profile, noticeboard, and events. The Parent section includes signup, login, viewing student profile, events, and any communication from the school. A parent can have more than one child in the school. The school Admin can manage the students' details (add, delete, update), create/update notifications, create/update events, and send communications to parents. The notifications, events, and other communications can be sent to individuals, a class, or the whole school cohort. 

All data is stored in a Firebase Cloud Firestore database. The application communicates with the Firebase console, which provides the system with the requested data from the database. Also, the application sends data to be stored in the database and be presented at a later stage.

## Further Details

The system has only one administrator. The administrator does not need to sign up. They are already signed up, as their details are added to the database in the ‘admin collection’, and just need to use their username and password to log in.

Each parent must have a unique phone number. As parent usernames are unique, these are used to uniquely identify each parent in the database.

Since the e-mail address of each student should be unique, this is used to uniquely identify each student in the database. Every valid student e-mail would be stored in the database, so when a student wishes to sign up and enters an e-mail address, the database is checked whether the entered e-mail is a recognised and valid student e-mail address. It is also checked if the entered e-mail is already used by another student. For each e-mail address, the student’s first name, last name, and class ID are also queried, so when the student signs up, the system knows who they are - their name and class. The administrator can send communications to either the parent of every student in the school, the parents of every student of a specific class, or the parents of some specific students. When events are created they are visible to every parent and student. We assumed that everyone can know every event, but the administrator can remind specific parents about events by using the Send Communications to Parents feature. Notifications are also visible to everyone, but if the administrator needs to send a notification reminder to a specific student’s parents, they can use the Send Communications to Parents feature to do so.

## Built With

* Kotlin
* XML

Other tools and technologies used:

* Firebase Cloud Firestore database