# call from project root via $ ruby remove-git-markers-in-localization-files.rb
Dir.glob("src/main/resources/l10n/*.properties") do |f|
  File.write(f, IO.readlines(f).map { |line| line.strip.start_with?("<<<<<<<") || line.strip.start_with?("=======") || line.strip.start_with?(">>>>>>>") ? "" : line }.join(""))
end
