require 'active_record'
require 'activerecord-jdbcmysql-adapter'

class Repository < ActiveRecord::Base
  establish_connection(
      adapter:  "mysql",
      host:     "10.176.34.84",
      username: "root",
      password: "root",
      database: "performance",
      encoding: "utf8mb4",
      collation: "utf8mb4_bin",
      pool: 20
  )
end