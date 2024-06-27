## About the Project

Currently in closed testing before release on Google Playstore.Written in Kotlin Compose and Android Studio Koala / Jellyfish. Developed for Android SDK 34 ( minimum SDK 26 ). Uses Kotlin Rooms and SQLite for referencing the local database. Created with Kotlin Flows and concurrency principles to improve performance. Uses the Shunting-Yard Algorithm for evaluating text. Intended to make it easier to play any games with complex dice rolls, such as Tabletop Roleplaying Games ( i.e. Dungeons and Dragons, Pathfinder, etc. ).

### Features: 

-Can create Expressions which each hold a piece of text which gets parsed for a result. 

-Expressions text can hold series of mathematical statements which can be evaluated for a result.

-Expressions can reference the value of other Expressions. They can either reference other Expressions locally / relatively or globally. Where a global reference, even when copied, will still refer to the same Expression as the original, a local reference will attempt to look for a new Expression at its new location.

-Groups can be used to Organize and Sort Expressions for a cleaner experience.

-Expressions and Groups can be Copy and Pasted ( **currently using placeholder UI elements** ) for reusability. When a Group is copied, all of the subgroups and expressions within it are also copied to the next location.

#### Syntax:

-Names of Expressions and Groups are case-sensitive and can only be made up of alphanumeric characters, -hyphens, and  _underscores.

-**Global Reference: "@(Group_FullPath/Expression_Name)" :**

This will reference the value of the Expression at that path. The fullpath of an Expression is determined by the Group fullpath, found at the top of the screen, and the name of the Expression. 
For example, the fullpath "@(Character/Stats/Strength)" will look for an Expression named "Strength" located in a Group named "Stats", which is located in a Group named "Character".

-**Local Reference: "@(../Expression_Name)" :**

This is the syntax for a Local or Relative Expression Reference, where the path of the Group containing the Expression replaces the "..". 
For example, if I have a group named "GroupA" which contains two Expressions: "ExpressionA" and "ExpressionB", ExpressionB can use "@(../ExpressionA)", which is the same as saying "@(GroupA/ExpressionA)", to reference ExpressionA. 
This is mainly useful simplifying references and for copying / pasting / templating Expressions and Groups ( templating not implemented yet ).


#### Current Functions:

-**floor ( x ):** This will round the "x" value down to the nearest Integer. For example, "ceil( 2.8 )" = 2.


-**ceil ( x ):** This will round the "x" value up to the nearest Integer. For example, "ceil( 2.1 )" = 3.


-**round ( x ):** This will round the "x" value to the nearest Integer, rounding on ties, where the decimal value is 0.5. For example, "round( 2.2 )" = 2, "round( 2.5 )" = 3, "round( 2.6 )" = 3.


-**min ( x, y ):** This will return the lower of the two values. For example, "max( 12, 8 )" = 8.


-**max ( x, y ):** This will return higher of the two values. For example, "max( 12, 8 )" = 12.


-**random ( x, y ):**
This will return a pseudo-random number between "x" (inclusive) and "y" (exclusive). This will also cause the Expression to be dynamic as this function will return a new value each time it is called. For example, random ( 0, 8 ) can return any of the following values: [0, 1, 2, 3 ,4 , 5, 6, 7].

-**roll ( x, y ):** 
This will return a roll "y" sided dice ( a random number between 1 (inclusive) and y (inclusive) ) "x" number of times. This will also cause the Expression to be *dynamic* as this function will return a new value each time it is called. 
For example, roll ( 3, 8 ) can return will return ( random ( 1, 9 ) + random ( 1, 9 ) + random ( 1, 9 )


### Planned Features ( Not Ordered ): 

-Better Selection for Copy & Paste Expressions / Groups

-Expression and Group Templates: Save a copy of an Expression or Group. If it is an Expression, then lock its text. If it is a Group, lock all of the Expressions and Groups that it contains, but allow the addition of new Expressions and Groups. 

-Path Navigation: Ability to click on hotlinks in the path shown for your current group to jump to the selected parent group.

-Advanced Expression Debugging: The selection of text in an Expression that is causing an error or warning is highlighted a corresponding color.

-Sharing / Community Templates

-Custom Tags: Ability to reference all Expressions that contain a specified tag.

-Arrays / Lists

-Loops / Iterators ( Maybe )

-Conditional Statements
