require 'repository'
require 'yaml'

module YmlParser
  def self.run
    repo_dir = File.expand_path(File.join("..", "..", "..", "sequence", "repository"), File.dirname(__FILE__))

    Repository.where('id > 0 AND contain_travis_yml = true').find_each do |repo|
      file_path = File.join(repo_dir, repo.repo_name.sub(/\//, '@'), ".travis.yml")
      p file_path
      thing = YAML.load_file(file_path)
      puts thing.inspect
    end
  end
end
YmlParser.run