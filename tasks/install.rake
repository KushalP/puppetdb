# Task to install puppetdb's files into a target directory
#
# DESTDIR is defined in the top-level Rakefile
# JAR_FILE is defined in the ext/tar.rake file
#
desc "Install PuppetDB (DESTDIR and PE_BUILD optional arguments)"
task :install => [  JAR_FILE  ] do
  unless File.exists?("ext/files/config.ini")
    Rake::Task[:template].invoke
  end

  require 'facter'
  raise "Oh damn. You need a newer facter or better facts. Facter version: #{Facter.version}" if Facter.value(:osfamily).nil?
  @osfamily = Facter.value(:osfamily).downcase
  mkdir_p "#{DESTDIR}/#{@install_dir}"
  mkdir_p "#{DESTDIR}/#{@config_dir}"
  mkdir_p "#{DESTDIR}/#{@config_dir}/.."
  mkdir_p "#{DESTDIR}/#{@log_dir}"
  mkdir_p "#{DESTDIR}/etc/init.d/"
  mkdir_p "#{DESTDIR}/#{@lib_dir}"
  mkdir_p "#{DESTDIR}/#{@libexec_dir}"
  mkdir_p "#{DESTDIR}/#{@sbin_dir}"
  mkdir_p "#{DESTDIR}/etc/logrotate.d/"
  ln_sf @config_dir, "#{DESTDIR}/#{@lib_dir}/config"
  ln_sf @log_dir, "#{DESTDIR}/#{@install_dir}/log"

  unless @pe
    mkdir_p "#{DESTDIR}/var/lib/puppetdb/state"
    mkdir_p "#{DESTDIR}/var/lib/puppetdb/db"
    mkdir_p "#{DESTDIR}/var/lib/puppetdb/mq"
    ln_sf "#{@lib_dir}/state", "#{DESTDIR}#{@link}/state"
    ln_sf "#{@lib_dir}/db", "#{DESTDIR}#{@link}/db"
    ln_sf "#{@lib_dir}/mq", "#{DESTDIR}#{@link}/mq"
    mkdir_p "#{DESTDIR}/etc/puppetdb"
  else
    mkdir_p "#{DESTDIR}#{@lib_dir}/state"
    mkdir_p "#{DESTDIR}#{@lib_dir}/db"
    mkdir_p "#{DESTDIR}#{@lib_dir}/mq"
    mkdir_p "#{DESTDIR}/etc/puppetlabs/puppetdb"
  end

  cp_p JAR_FILE, "#{DESTDIR}/#{@install_dir}"
  cp_pr "ext/files/config.ini", "#{DESTDIR}/#{@config_dir}"
  cp_pr "ext/files/database.ini", "#{DESTDIR}/#{@config_dir}"
  cp_pr "ext/files/jetty.ini", "#{DESTDIR}/#{@config_dir}"
  cp_pr "ext/files/repl.ini", "#{DESTDIR}/#{@config_dir}"
  cp_pr "ext/files/puppetdb.logrotate", "#{DESTDIR}/etc/logrotate.d/#{@name}"
  cp_pr "ext/files/log4j.properties", "#{DESTDIR}/#{@config_dir}/.."
  cp_pr "ext/files/puppetdb", "#{DESTDIR}/#{@sbin_dir}"

  # Copy legacy wrapper for deprecated hyphenated sub-commands
  legacy_cmds=%w|puppetdb-ssl-setup puppetdb-foreground puppetdb-import puppetdb-export puppetdb-anonymize|
  legacy_cmds.each do |file|
    cp_pr "ext/files/puppetdb-legacy", "#{DESTDIR}/#{@sbin_dir}/#{file}"
  end

  # Copy internal sub-commands to libexec location
  internal_cmds=legacy_cmds
  internal_cmds.each do |file|
    cp_pr "ext/files/#{file}", "#{DESTDIR}/#{@libexec_dir}"
  end

  # figure out which init script to install based on facter
  if @osfamily == "redhat"
    @operatingsystem = Facter.value(:operatingsystem).downcase
    @operatingsystemrelease = `cat /etc/redhat-release | awk '{print $3}'`.chomp
    puts "operatingsystem is #{@operatingsystem}"
    puts "operatingsystemrelease is #{@operatingsystemrelease}"
    if (@operatingsystem == "fedora" && @operatingsystemrelease.to_i >= 17) || (@operatingsystem =~ /redhat|centos/ && @operatingsystemrelease.to_f >= 7 )
      #systemd!
      mkdir_p "#{DESTDIR}/usr/lib/systemd/system"
      cp_p "ext/files/systemd/#{@name}.service", "#{DESTDIR}/usr/lib/systemd/system"
      chmod 0644, "#{DESTDIR}/usr/lib/systemd/system/#{@name}.service"
    else
      mkdir_p "#{DESTDIR}/etc/sysconfig"
      mkdir_p "#{DESTDIR}/etc/rc.d/init.d/"
      cp_p "ext/files/puppetdb.default", "#{DESTDIR}/etc/sysconfig/#{@name}"
      cp_p "ext/files/puppetdb.redhat.init", "#{DESTDIR}/etc/rc.d/init.d/#{@name}"
      chmod 0755, "#{DESTDIR}/etc/rc.d/init.d/#{@name}"
    end
  elsif @osfamily == "suse"
    mkdir_p "#{DESTDIR}/etc/sysconfig"
    mkdir_p "#{DESTDIR}/etc/init.d/"
    cp_p "ext/files/puppetdb.default", "#{DESTDIR}/etc/sysconfig/#{@name}"
    cp_p "ext/files/puppetdb.suse.init", "#{DESTDIR}/etc/init.d/#{@name}"
    chmod 0755, "#{DESTDIR}/etc/init.d/#{@name}"
  elsif @osfamily == "debian"
    mkdir_p "#{DESTDIR}/etc/default"
    cp_p "ext/files/puppetdb.default", "#{DESTDIR}/etc/default/#{@name}"
    cp_pr "ext/files/#{@name}.debian.init", "#{DESTDIR}/etc/init.d/#{@name}"
    chmod 0755, "#{DESTDIR}/etc/init.d/#{@name}"
  elsif @osfamily == "openbsd"
    mkdir_p "#{DESTDIR}/etc/rc.d/"
    cp_p "ext/files/puppetdb.openbsd.init", "#{DESTDIR}/etc/rc.d/#{@name}.rc"
    chmod 0755, "#{DESTDIR}/etc/rc.d/#{@name}.rc"
  else
    raise "Unknown or unsupported osfamily: #{@osfamily}"
  end
  chmod 0750, "#{DESTDIR}/#{@config_dir}"
  chmod 0640, "#{DESTDIR}/#{@config_dir}/../log4j.properties"
  chmod 0700, "#{DESTDIR}/#{@sbin_dir}/puppetdb-ssl-setup"
  chmod 0700, "#{DESTDIR}/#{@sbin_dir}/puppetdb-foreground"
  chmod 0700, "#{DESTDIR}/#{@sbin_dir}/puppetdb-import"
  chmod 0700, "#{DESTDIR}/#{@sbin_dir}/puppetdb-export"
  chmod 0700, "#{DESTDIR}/#{@sbin_dir}/puppetdb-anonymize"
  chmod 0700, "#{DESTDIR}/#{@sbin_dir}/puppetdb"
end
