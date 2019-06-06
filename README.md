Gobblet Noob
===========

Gobblet Noob is an Android app that plays the game Gobblet.
Originally built in 2012 using the Android Development Toolkit in Eclipse,
the Gobblet Noob app is currently built with Android Studio Version 3.4.1.
The game of Gobblet was designed by Thierry Denoual.

A description of the app and a down-loadable APK are available at my Projects Website:

https://kit-barnes.github.io/Kits_Project_Website

The three Java source files for Gobblet Noob have been unchanged since 2012

- Game.java contains all the logic for the game itself, its rules, state,
and selection of moves for the computer opponents.
- GobbletView.java contains the user interface for the playing area,
including drawing, response to touch events, and animation.
- Gobblet.java implements the main activity of the app,
gluing together the game and the view,
saving and restoring the state of the app,
implementing the menus and dialogs,
and keeping track of and showing game status.
