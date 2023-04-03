require 'java_repository'
require 'repository'

require 'travis'
require 'activerecord-import'

require 'open-uri'
require 'find'
module ScanRepo
  @dir_path = File.expand_path(File.join("..", "..", "..", "bodyLog2", "repo_json_files"), File.dirname(__FILE__))

  def self.useTravis()
    Thread.abort_on_exception = true
    queue = SizedQueue.new(200)
    consumer = Thread.new do
      id = 0
      loop do
        bulk = []
        hash = nil
        200.times do
          hash = queue.deq
          break if hash == :END_OF_WORK
            #bulk << Repository.new(hash)
        end
        #Repository.import bulk
        break if hash == :END_OF_WORK
      end
    end

    Travis.access_token = "C-cYiDyx1DUXq3rjwWXmoQ"

    JavaRepository.where("id < 100").find_each do |jrepo|
      repo_name = jrepo.owner_name + "/" + jrepo.repos_name
      p "#{jrepo.id} #{repo_name}"
      begin
        repo = Travis::Repository.find(repo_name)
      rescue
        p $!
        repo = nil
      end
      p repo
      hash = Hash.new
      hash[:id] = jrepo.id
      hash[:repo_name] = repo_name
      hash[:star_number] = jrepo.stars_count
      hash[:use_travis] = repo.nil? ? false : true
      queue.enq hash
    end
    queue.enq(:END_OF_WORK)
    consumer.join
    puts "Scan Over"
  end

  def self.crawl(id, url)
    j = nil
    begin
      open(url, "User-Agent" => "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36 Edg/91.0.864.59",
           'Authorization'=>'token TW8mt5AdDagMyZHgOx5YQQ') { |f| j = JSON.parse(f.read) }
    rescue
      puts "Failed to get the info of #{id} #{url}: #{$!}"
      #sleep 20
      #count += 1
      #retry if count<50
      #return
    end
    return if j.nil?
    file_name = File.join(@dir_path, "#{id}@#{j['slug'].sub(/\//,'@')}.json")
    p id unless j["last_build_id"].nil?
    unless File.size?(file_name)
      File.open(file_name, 'w') do |file|
        file.puts(JSON.pretty_generate(j))
      end
    end
  end

  def self.max_buildnum
    FileUtils.mkdir_p(@dir_path) unless File.exist?(@dir_path)
    Thread.abort_on_exception = true
    threads_num = 32
    queue = SizedQueue.new(200)
    threads = []
    threads_num.times do
      thread = Thread.new do
        loop do
          hash = queue.deq
          break if hash == :END_OF_WORK
          crawl(hash[:id], hash[:url])
        end
      end
      threads << thread
    end

    Repository.where("use_travis = true and id > 2243").find_each do |repo|
      id = repo.id
      repo_name = repo.repo_name
      url = "https://api.travis-ci.org/repos/#{repo_name}"
      #p url
      hash = Hash.new
      hash[:id] = id
      hash[:url] = url
      queue.enq hash
    end

    threads_num.times do
      queue.enq :END_OF_WORK
    end
    threads.each { |t| t.join }
    puts "Scan Over"
  end

  def self.parse_json(file_path)
    hash = Hash.new
    #p file_path
    match = /(\d+)@(.+)@(.+)\.json/.match(file_path)
    hash[:id] = match[1].to_i
    hash[:repo_name] = match[2] + '/' + match[3]
    j = JSON.parse IO.read(file_path)
    hash[:last_build_number] = j['last_build_number'] ? j['last_build_number'].to_i : 0
    hash[:last_build_started_at] = j['last_build_started_at'] ? DateTime.parse(j['last_build_started_at']).new_offset(0) : nil
    hash
  end

  def self.parse_repo_info
    Thread.abort_on_exception = true
    in_queue = SizedQueue.new(200)
    out_queue = SizedQueue.new(200)
    consumer = Thread.new do
      id = 0
      loop do
        bulk = []
        hash = nil
        200.times do
          hash = out_queue.deq
          break if hash == :END_OF_WORK
          bulk << Repository.new(hash)
        end
        Repository.import bulk
        break if hash == :END_OF_WORK
      end
    end

    threads_num = 32
    threads = []
    threads_num.times do
      thread = Thread.new do
        loop do
          file_path = in_queue.deq
          break if file_path == :END_OF_WORK
          hash = parse_json(file_path)
          out_queue.enq hash
        end
      end
      threads << thread
    end

    Dir.foreach(@dir_path) do |file_name|
      next if file_name == '.' or file_name == '..' or file_name == '.DS_Store'
      file_path = File.join(@dir_path, file_name)
      in_queue.enq file_path
    end

    threads_num.times do
      in_queue.enq :END_OF_WORK
    end
    threads.each { |t| t.join }
    out_queue.enq(:END_OF_WORK)
    consumer.join
    puts "Scan Over"
  end

  #检测项目代码仓库是否包含travis文件
  def self.check_travis_yml
    repo_dir = File.expand_path(File.join("..", "..", "..", "sequence", "repository"), File.dirname(__FILE__))
    Repository.where('id > 0 AND star_number>=25 AND last_build_number>=50').find_each do |repo|
      repo_name = repo.repo_name
      repo_path = File.join(repo_dir, repo_name.sub(/\//, '@'))
      p repo.id
      contain_travis_yml = false
      #tool 0: 没用maven gradle 1: maven 2: gradle groovy 3: gradle kotlin 4: maven gradle都有
      tool = 0
      if File.exist?(repo_path)
        file_names = []
        Dir.foreach(repo_path) do |file_name|
          file_names << file_name
        end
        if file_names.include? ".travis.yml"
          contain_travis_yml = true
        end

        if file_names.include? "pom.xml" && (file_names.include?("build.gradle") || file_names.include?("build.gradle.kts"))
          tool = 4
        elsif file_names.include? "pom.xml"
          tool = 1
        elsif file_names.include? "build.gradle"
          tool = 2
        elsif file_names.include? "build.gradle.kts"
          tool = 3
        end
      end
      repo.contain_travis_yml = contain_travis_yml
      repo.build_tool = tool
      repo.save
    end
  end

  def self.check_last_build_time
    repo_dir = File.expand_path(File.join("..", "..", "..", "sequence", "repository"), File.dirname(__FILE__))
    Repository.where('contain_travis_yml = true and last_build_time is null').find_each do |repo|
      repo_name = repo.repo_name
      p repo_name
      tr = Travis::Repository.find(repo_name)
      last_build = tr.last_build
      next if last_build.nil?
      last_build_num = last_build.number.to_i

      last_build_time = nil
      tr.each_build.each do |build|
        p build.number
        if build.pull_request == false
          last_build_time = build.started_at
          break
        end
      end

      # if last_build.pull_request == false
      #   repo.last_build_time = last_build.started_at
      #   repo.save
      # end
      if last_build_time.nil? == false
        repo.last_build_time = last_build_time
        repo.save
      end

    end
  end

  def self.check_build_tool
    repo_dir = File.expand_path(File.join("..", "..", "..", "sequence", "repository"), File.dirname(__FILE__))
    Repository.where('contain_travis_yml = true and build_tool = 0').find_each do |repo|
      repo_name = repo.repo_name
      repo_path = File.join(repo_dir, repo_name.sub(/\//, '@'))
      p repo_name
      #tool 0: 没用maven gradle 1: maven 2: gradle groovy 3: gradle kotlin 4: maven gradle都有
      tool = 0
      if File.exist?(repo_path)
        file_names = []
        Find.find(repo_path) do |file_name|
          if file_name.end_with?('pom.xml') || file_name.end_with?('.gradle') || file_name.end_with?('.gradle.kts')
            file_names << File.basename(file_name)
          end
        end
        p file_names
        if file_names.include? "pom.xml" && (file_names.include?("build.gradle") || file_names.include?("build.gradle.kts"))
          tool = 4
        elsif file_names.include? "pom.xml"
          tool = 1
        elsif file_names.include? "build.gradle"
          tool = 2
        elsif file_names.include? "build.gradle.kts"
          tool = 3
        end
        p tool
      end
      repo.build_tool = tool
      repo.save
    end
  end

  def self.recheck_build_tool
    repo_dir = File.expand_path(File.join("..", "..", "..", "sequence", "repository"), File.dirname(__FILE__))
    Repository.where('contain_travis_yml = true and build_tool = 1').find_each do |repo|
      repo_name = repo.repo_name
      repo_path = File.join(repo_dir, repo_name.sub(/\//, '@'))
      #tool 0: 没用maven gradle 1: maven 2: gradle groovy 3: gradle kotlin 4: maven gradle都有
      tool = 0
      if File.exist?(repo_path)
        file_names = []
        Dir.foreach(repo_path) do |file_name|
          file_names << file_name
        end
        if repo_name == 'rapidftr/RapidFTR-Android'
          p file_names
          p file_names.include? "pom.xml"

        end
        if file_names.include?("pom.xml") == false
          p repo_name
        end
      end
      #repo.build_tool = tool
      #repo.save
    end
  end


end

#ScanRepo.useTravis
#ScanRepo.max_buildnum
#ScanRepo.parse_repo_info
#ScanRepo.check_travis_yml
#ScanRepo.check_last_build_time
#ScanRepo.check_build_tool
ScanRepo.recheck_build_tool