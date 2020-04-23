describe "The file \"locales.json\"" do
  before(:all) do
    locales_file_path = File.join("#{LOCALE_ROOT}", "locales.json")
    @locales_file_exists = File.exist?(locales_file_path)
    
    @locales_file_validates = false
    begin
      locales = JSON.parse(File.read(locales_file_path))
      @locales_file_validates = true
    rescue JSON::ParserError => e
    end
    
    @primary_dialects = {}
    @language_names = {}
    
    if @locales_file_validates
      @primary_dialects = locales["primary-dialects"]
      @language_names = locales["language-names"]
    end
    
    # Store locales of locale files (based on their file names)
    @locale_file_locales = Dir[File.join(LOCALE_ROOT, 'locales-*.xml')].map { |path|
      filename = File.basename(path)
      locale = filename[8..-5]
    }
    @locale_file_languages = @locale_file_locales.map { |locale| language = locale[0..1] }
    @locale_file_languages.uniq!
  end

  it "must be present" do
    expect(@locales_file_exists).to be true
  end

  it "must be valid JSON" do
    if @locales_file_exists
      expect(@locales_file_validates).to be true
    end
  end
  
  it "must define a primary dialect for every language (e.g. \"de-DE\" for \"de\")" do
    expect(@locale_file_languages - @primary_dialects.keys).to eq([])
  end
  
  it "must define language names for every locale" do
    expect(@locale_file_locales - @language_names.keys).to eq([])
  end
  
  it "must define two language names for every locale (in the language itself and in English)" do
    incorrect_entries = @language_names.select { |locale, descriptions| descriptions.length != 2 }
    expect(incorrect_entries).to eq({})
  end
end

