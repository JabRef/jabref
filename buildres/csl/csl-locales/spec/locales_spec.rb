Locales.each_pair do |id, (filename, path, locale)|

  describe "#{id}" do

    it "is a valid CSL 1.0.1 locale" do
      expect(CSL.validate(path)).to eq([])
    end

    it "has a conventional file name" do
      expect(filename).to match(/^locales-[a-z]{2}(-[A-Z]{2})?\.xml$/)
    end

    it "was successfully parsed" do
      expect(locale).to be_a(CSL::Locale)
    end

    unless locale.nil?
      it "has an info element" do
       expect(locale).to have_info
      end

      it "has a language" do
        expect(locale.language).not_to be_empty
      end

      it "has a region" do
        expect(locale.region).not_to be_empty
      end unless NO_REGIONS.include?(locale.language.to_s)

      it "its language and region match the filename" do
        expect(locale.to_s).to eq(id[8,5])
      end

      it "has and info/rights element" do
        expect(locale.info).to have_rights
      end

      it "is licensed under a CC BY-SA license" do
        expect(locale.info).to be_default_license
      end
    end

  end
end
