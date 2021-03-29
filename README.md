## Group Creator

This code takes student preferences into account for project and builds groups from those that are optimal. It was one of my first Scala 3 projects to get familiar with the changes.

The preferences text file should list students and their preferences for each project. Projects students are most interested in should be given a value of 1 and this code minimizes the sum while cutting off the search at a particular value.

### Update notes

The current algorithm to pick what projects should be thrown out seems like a reasonable heuristic, but it failed on the settings where I first used this. That is why the dropped groups are currently hard coded.

Given that this works very fast when only top selections are allowed, I think I should change it so that it tries all combination of projects to drop and increments the worst selection allowed until a solution is found.

Note that if this change is made, all student options need to be "normalized" so that their top choice is always a 1. This way a student who gives every project low priority doesn't cause problems.