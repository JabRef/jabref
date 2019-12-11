shared_examples "style" do |basename, (filename, path, style, error), in_dependent_subdir|

  it "must validate against the CSL 1.0.1 schema
     Please check your style at http://validator.citationstyles.org/" do
    expect(CSL.validate(path)).to be_empty
  end

  it "must be parsable as a CSL style" do
    expect(style).to be_a(CSL::Style), error.to_s
  end

  it "must have a conventional file name" do
    expect(filename).to match(/^[a-z\d]+(-[a-z\d]+)*\.csl$/)
  end

  unless style.nil?
    if !in_dependent_subdir
      it 'must be an independent style (dependent styles must be placed in the "dependent" subdirectory)' do
        expect(style).to be_independent
      end

      it 'must have a "self" link' do
        expect(style).to have_self_link
      end

      it '"template" link must point to an existing independent style' do
        if style.has_template_link?
          template_link = style.template_link
          link_prefix = "http://www.zotero.org/styles/"

          expect(template_link).to match(%r{^#{link_prefix}})
          template_ID = template_link[link_prefix.length..-1]

          expect(INDEPENDENTS_BASENAMES).to include(template_ID)
        end
      end

      unless CITATION_FORMAT_FILTER.include?(basename)

        it 'must define a citation-format (<category citation-format="..."/>)' do
          expect(style.citation_format).not_to be_nil
        end

        it 'must have a "class" attribute that matches the "citation-format" attribute' do
          if style.citation_format == :note
            expect(style[:class]).to eq('note')
          else
            expect(style[:class]).to eq('in-text')
          end
        end
      end

      it "must define all macros that are called by <text/> and <key/> elements" do
        style.descendants!.each do |node|
          if node.matches?(/^key|text$/, :macro => /./)
            expect(style.macros).to have_key(node[:macro])
          end
        end
      end

      unless UNUSED_MACROS_FILTER.include?(basename)
        it "may not have any unused macros" do
          available_macros = style.macros.keys.sort

          used_macros = style.descendants.
            select { |node| node.attribute? :macro }.
            map    { |node| node[:macro] }.
            sort.uniq

          expect(available_macros - used_macros).to eq([])
        end
      end

      it "must not use sentence case for title and container-title variables" do
        style.descendants!.each do |node|
          if node.matches?("text")
            if node[:'text-case'] == 'sentence'
              expect(node[:'variable']).not_to match(/^title|container-title$/)
            end
          end
        end
      end

      describe "name nodes" do
        it "must have valid et-al-min and et-al-use-first attributes" do
          style.each_descendant do |node|
            if node.is_a? CSL::Style::Names

              # Make a copy of the name node and inherit options from root
              # and citation/bibliography depending on node's location.
              if node.has_name?
                name = node.name.dup
              else
                name = CSL::Style::Name.new
              end

              parents = [node.closest(/^(citation|bibliography|macro)$/)]

              # For macros, check inheritance for both rendering modes!
              if parents[0].is_a? CSL::Style::Macro
                parents = [style.citation, style.bibliography]
              end

              parents.each do |parent|
                nn = name.dup.reverse_merge!(name.inherited_name_options(parent, style))

                # We expect both attributes to return be values
                # of the same type: either String or NilClass.
                # Using #fetch here resolves inherited values!
                min = nn.attributes.fetch(:'et-al-min')
                first = nn.attributes.fetch(:'et-al-use-first')

                expect(min).to be_an_instance_of(first.class),
                  "expected et-al-min (#{min}) and et-al-use-first (#{first}) to be of same type"

                unless min.nil?
                  expect(min.to_i).to be > first.to_i,
                    "expected et-al-min (#{min}) to be greater than et-al-use-first (#{first})"
                end

                min = nn.attributes.fetch(:'et-al-subsequent-min')
                first = nn.attributes.fetch(:'et-al-subsequent-use-first')

                expect(min).to be_an_instance_of(first.class),
                  "expected et-al-subsequent-min (#{min}) and et-al-subsequent-use-first (#{first}) to be of same type"

                unless min.nil?
                  expect(min.to_i).to be > first.to_i,
                    "expected et-al-subsequent-min (#{min}) to be greater than et-al-subsequent-use-first (#{first})"
                end
              end
            end
          end
        end
      end

    end

    if in_dependent_subdir
      it "must be a dependent style (independent styles must be placed in the root directory)" do
        expect(style).to be_dependent
      end

      it '"independent-parent" link must point to an existing independent style' do
        parent_ID_link = style.independent_parent_link
        link_prefix = "http://www.zotero.org/styles/"

        expect(parent_ID_link).to match(%r{^#{link_prefix}})
        parent_ID = parent_ID_link[link_prefix.length..-1]

        expect(INDEPENDENTS_BASENAMES).to include(parent_ID)
      end

      it 'may not have <macro/>, <citation/>, or <bibliography/> elements' do
        expect(style).not_to have_macro
        expect(style).not_to have_citation
        expect(style).not_to have_bibliography
      end

      it 'may not have a "template" link' do
        expect(style).not_to have_template_link
      end

      it 'must define a citation-format (<category citation-format="..."/>)' do
        expect(style.citation_format).not_to be_nil
      end

      it "must have the same citation-format as its independent-parent" do
        parent = style.independent_parent_link[/[^\/]+$/]
        parent = Independents[parent][-1]

        expect(style.citation_format).to eq(parent.citation_format)
      end
    end

    it "must have an <info/> element" do
     expect(style).to have_info
    end

    it "must have a style ID" do
      expect(style.info).to have_id
    end

    it 'style ID must be of the form "http://www.zotero.org/styles/" + style file name (without ".csl" extension, e.g. "http://www.zotero.org/styles/apa")' do
      expect(style.id).to eq("http://www.zotero.org/styles/#{basename}")
    end

    it '"self" link must match the style ID' do
      if style.has_self_link?
        expect(style.id).to eq(style.self_link)
      end
    end

    it "must have a <rights> element" do
      expect(style.info).to have_rights
    end

    it "must have the correct Creative Commons BY-SA license URL" do
      if style.info.has_rights?
       expect(style.info.rights[:license]).to eq(CSL_LICENSE_URL)
      end
    end

    it "must have the correct Creative Commons BY-SA license text" do
      if style.info.has_rights?
        expect(style.info.rights.text).to eq(CSL_LICENSE_TEXT)
      end
    end

  end
end

Independents.each_pair do |basename, (filename, path, style, error)|
  in_dependent_subdir = false
  describe "#{basename}:" do
    include_examples "style", basename, [filename, path, style, error], in_dependent_subdir
  end
end

Dependents.each_pair do |basename, (filename, path, style, error)|
  in_dependent_subdir = true
  describe "dependent/#{basename}:" do
    include_examples "style", basename, [filename, path, style, error], in_dependent_subdir
  end
end
