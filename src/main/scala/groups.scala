
import scala.io.Source

case class StudentData(name: String, prefs: Array[Int]) {
  def dropProjects(groupsToDrop: Seq[Int]): StudentData = {
    copy(prefs = groupsToDrop.foldLeft(prefs)((prs, i) => prs.patch(i, Nil, 1)))
  }
}

case class GroupAssignments(groups: IndexedSeq[Set[Int]])

@main def groups(fileName: String, numProjects: Int, totalGroups: Int, worstRating: Int, groupTolerance: Int): Unit = {
  val (projects, students) = readData(fileName, numProjects, worstRating)
  val numToDrop = numProjects - totalGroups
  val groupsToDrop = Array(7, 4) // badProjects(students, numToDrop, totalGroups).sorted.reverse
  println(s"Dropping ${groupsToDrop.map(projects).mkString(", ")}")
  val safeProjects = groupsToDrop.foldLeft(projects)((projs, i) => projs.patch(i, Nil, 1))
  println(safeProjects.mkString(", "))
  val updatedStudents = students.map(_.dropProjects(groupsToDrop))
  updatedStudents.foreach(s => println(s"${s.name}: ${s.prefs.mkString(", ")}"))
  val groupSize = students.length / totalGroups
  val (score, groups) = makeGroups(updatedStudents, worstRating, groupSize, groupTolerance)
  println(score)
  for ((ss, i) <- groups.groups.zipWithIndex) {
    println(s"${safeProjects(i)}: ${ss.map(i => students(i).name).mkString(", ")}")
  }
}

def readData(fileName: String, numProjects: Int, worstRating: Int): (Array[String], Array[StudentData]) = {
  val source = Source.fromFile(fileName)
  val lines = source.getLines()
  val names = lines.take(numProjects).toArray
  val students = lines.grouped(numProjects+1).map(s => StudentData(s.head, s.tail.map(_.toInt).toArray)).toArray
  source.close()
  (names, students.sortBy(-_.prefs.count(_ >= worstRating)))
}

def badProjects(students: Array[StudentData], numToDrop: Int, numGroups: Int): IndexedSeq[Int] = {
  val indices = students.head.prefs.indices
  val numOnes = indices.map(pIndex => students.count(_.prefs(pIndex) == 1))
  val numTwos = indices.map(pIndex => students.count(_.prefs(pIndex) == 2))
  val totalScore = indices.map(pIndex => students.map(_.prefs(pIndex)).sum)
  val pass12 = (numOnes lazyZip numTwos lazyZip indices)
    .filter((one, two, i) => one + two < numGroups).map((one, two, i) => i)
  println(s"pass12 = $pass12")
  if (pass12.length == numToDrop) pass12
  else if (pass12.length < numToDrop) {
    println("A")
    pass12 ++ (totalScore zip indices).sortBy(_._1).filter(t => !pass12.contains(t._2)).dropRight(numToDrop - pass12.length).map(_._2)
  } else {
    val pass1 = (numOnes lazyZip indices)
      .filter((one, i) => one < numGroups).map((one, i) => i)
    println(s"pass1 = $pass1")
    if (pass1.length == numToDrop) pass1
    else if (pass1.length < numToDrop) {
      println("B")
      pass1 ++ (totalScore zip indices).sortBy(_._1).filter(t => !pass1.contains(t._2)).takeRight(numToDrop - pass1.length).map(_._2)
    } else {
      println("C")
      (totalScore zip indices).filter(t => pass1.contains(t._2)).sortBy(_._1).map(_._2).takeRight(numToDrop)
    }
  }
}

def makeGroups(students: Array[StudentData], worstRating: Int, groupSize: Int, groupTolerance: Int): (Int, GroupAssignments) = {
  def helper(groups: GroupAssignments, student: Int): (Int, GroupAssignments) = {
    if (student >= students.length) {
      // println(s"Found $groups")
      // groups.groups.zipWithIndex.foreach { (g, i) => println(g.map(si => s"${students(si).name} ${students(si).prefs(i)}").mkString(", "))}
      // println(groups.groups.zipWithIndex.map { (g, i) => g.toSeq.map(si => students(si).prefs(i)).sum}.sum)
      (groups.groups.zipWithIndex.map { (g, i) => g.toSeq.map(si => students(si).prefs(i) * students(si).prefs(i)).sum}.sum, groups)
    } else {
      var ret = (1000000000, GroupAssignments(IndexedSeq()))
      for (gi <- groups.groups.indices) {
        if (students(student).prefs(gi) <= worstRating && groups.groups(gi).size < groupSize + groupTolerance) {
          val cur = helper(groups.copy(groups = groups.groups.updated(gi, groups.groups(gi) + student)), student + 1)
          if (cur._1 < ret._1) ret = cur
        }
      }
      ret
    }
  }
  helper(GroupAssignments(IndexedSeq.fill(students.head.prefs.length)(Set.empty[Int])), 0)
}