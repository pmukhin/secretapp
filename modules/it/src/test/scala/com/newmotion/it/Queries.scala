package com.newmotion.it

import doobie.util.fragment.Fragment

trait Queries {
  import doobie.implicits._

  protected val allTables: doobie.ConnectionIO[List[String]] =
    sql"SHOW TABLES".query[String].to[List]

  // no clue why but works only this way
  // sql"TRUNCATE TABLE $table" fails
  protected def truncate(table: String): doobie.ConnectionIO[Int] =
    Fragment.const(s"TRUNCATE TABLE $table").update.run
}
