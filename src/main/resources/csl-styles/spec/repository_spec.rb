describe "The CSL Style Repository" do

  unless ENV['CSL_TEST']
    it "must contain independent styles" do
      expect(Independents).not_to be_empty
    end

    it "must contain dependent styles" do
      expect(Dependents).not_to be_empty
    end
  end

  it "may not contain any duplicate file names" do
    expect(Independents.keys & Dependents.keys).to eq([])
  end

  it "may not contain any (non-excepted) duplicate ISSNs" do
    expect(ISSN.select { |_, styles| styles.length > 1 }).to eq({})
  end

  it "may not contain any duplicate style titles" do
    expect(TITLES.select { |_, styles| styles.length > 1 }).to eq({})
  end

  it 'may not contain extra files (make sure styles have a ".csl" extension)' do
    expect(EXTRA_FILES).to eq([])
  end
end

describe "The file \"renamed-styles.json\"" do
  before(:all) do
    renamed_styles_file_path = File.join("#{STYLE_ROOT}", "renamed-styles.json")
    @renamed_styles_file_exists = File.exist?(renamed_styles_file_path)
    
    @renamed_styles_file_validates = false
    begin
      @renamed_styles = JSON.parse(File.read(renamed_styles_file_path))
      @renamed_styles_file_validates = true
    rescue JSON::ParserError => e
    end
    
    @renamed_styles_entries = []
    @renamed_styles_targets = []
    
    if @renamed_styles_file_validates
      @renamed_styles_entries.push(*@renamed_styles.keys)
      @renamed_styles_targets.push(*@renamed_styles.values)
    end
  end

  it "must be present" do
    expect(@renamed_styles_file_exists).to be true
  end

  it "must be valid JSON" do
    if @renamed_styles_file_exists
      expect(@renamed_styles_file_validates).to be true
    end
  end
  
  it "may not contain entries for styles present in the repository" do
    expect(@renamed_styles_entries & (INDEPENDENTS_BASENAMES + DEPENDENTS_BASENAMES)).to eq([])
  end
  
  it "may not redirect to styles not present in the repository" do
    expect(@renamed_styles_targets - (INDEPENDENTS_BASENAMES + DEPENDENTS_BASENAMES)).to eq([])
  end
end
