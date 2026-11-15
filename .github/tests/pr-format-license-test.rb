require "minitest/autorun"
require "open3"
require "yaml"

class PrFormatLicenseTest < Minitest::Test
  LICENSE_ITEM = "- [x] I own the copyright of the code submitted and I license it under the [MIT license](https://github.com/JabRef/jabref/blob/main/LICENSE)".freeze
  WORKFLOW_PATH = File.expand_path("../workflows/pr-format.yml", __dir__)

  def setup
    @job = YAML.load_file(WORKFLOW_PATH).fetch("jobs").fetch("license-checked")
    @script = @job.fetch("steps").first.fetch("run").gsub('${{ github.event.number }}', "123")
  end

  def test_license_confirmation_requires_exactly_one_checked_item
    refute @job.key?("needs"), "The license gate must run when the Checklist job fails"

    unchecked_item = LICENSE_ITEM.sub("[x]", "[ ]")
    other_items = (1..5).map { |number| "- [x] Other item #{number}" }
    cases = [
      ["missing section", "### Other", false, "Checklist section is missing"],
      ["missing declaration", "### Checklist\n- [x] Other item", false, "found 0"],
      ["unchecked declaration", "### Checklist\n#{unchecked_item}", false, "must be ticked"],
      ["checked declaration", "### Checklist\n#{LICENSE_ITEM}", true, "MIT license confirmed"],
      ["seven boxes with duplicate declarations", (["### Checklist", unchecked_item, LICENSE_ITEM] + other_items).join("\n"), false, "found 2"],
      ["duplicate after twenty lines", (["### Checklist", LICENSE_ITEM] + Array.new(25, "") + [unchecked_item]).join("\n"), false, "found 2"]
    ]

    cases.each do |name, body, expected_success, expected_message|
      output, error, status = Open3.capture3({"TEST_BODY" => body}, "bash", "-c", 'gh() { printf "%s\n" "$TEST_BODY"; }' + "\n" + @script)

      assert_equal expected_success, status.success?, "#{name}: #{output}#{error}"
      assert_includes output, expected_message, name
    end
  end
end
