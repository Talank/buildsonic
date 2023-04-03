require 'repository'

module CloneRepo
  @repo_path = File.expand_path(File.join("..", "..", "..", "sequence", "repository"), File.dirname(__FILE__))
  @thread_number = 5

  def self.spwn(cmd, repo_path)
    result = system(cmd)
    if result == false
      cmd = "cd #{repo_path} && git pull"
      result = system(cmd)
    end
  end

  def self.thread_init
    @queue = SizedQueue.new(@thread_number)
    threads = []
    @thread_number.times do
      thread = Thread.new do
        loop do
          array = @queue.deq
          break if array == :END_OF_WORK
          spwn(array[0], array[1])
        end
      end
      threads << thread
    end
    threads
  end

  def self.run
    Thread.abort_on_exception = true
    threads = thread_init
    #repo_names = ["EngineHub/CraftBook"]
    Repository.where('id > 0 AND star_number>=25 AND last_build_number>=50').find_each do |repo|
      repo_name = repo.repo_name
      puts "#{repo.id}: #{repo_name}"
      repo_path = File.join(@repo_path, repo_name.sub(/\//, '@'))
      cmd = "git clone git://github.com/#{repo_name}.git #{repo_path}"
      @queue.enq [cmd, repo_path]
    end

    @thread_number.times do
      @queue.enq :END_OF_WORK
    end
    threads.each { |t| t.join }
    puts "Clone Over"
  end

  def self.wrong_repo
    file_path = File.expand_path(File.join("..", "..", "resources", "wrong_repo_list.txt"), File.dirname(__FILE__))
    repo_list = IO.readlines(file_path).collect! { |f| f.chomp }
    p repo_list
    repo_list.each do |repo_name|
      repo_path = File.join(@repo_path, repo_name.sub(/\//, '@'))
      p repo_path
      cmd = "rm -rf #{repo_path}"
      result = system(cmd)
      cmd = "git clone git://github.com/#{repo_name}.git #{repo_path}"
      result = system(cmd)
    end
  end
end
CloneRepo.run
#CloneRepo.wrong_repo