module Pull
  @thread_number = 5

  def self.spwn(repo_path)
    cmd = "cd #{repo_path} && git pull"
    result = system(cmd)
  end

  def self.thread_init
    @queue = SizedQueue.new(@thread_number)
    threads = []
    @thread_number.times do
      thread = Thread.new do
        loop do
          repo_path = @queue.deq
          break if repo_path == :END_OF_WORK
          spwn(repo_path)
        end
      end
      threads << thread
    end
    threads
  end

  def self.run
    Thread.abort_on_exception = true
    threads = thread_init
    dir_path = File.expand_path(File.join("..", "..", "sequence", "repository"), File.dirname(__FILE__))
    repo_paths = []
    Dir.glob("#{dir_path}/*").each do |path|
      repo_paths << path if File.directory?(path) && path.include?("@")
    end

    repo_paths.each do |repo_path|
      @queue.enq repo_path
    end

    @thread_number.times do
      @queue.enq :END_OF_WORK
    end
    threads.each { |t| t.join }
    puts "Clone Over"
  end
end

Pull.run