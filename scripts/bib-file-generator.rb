number_of_entries = 100_000

1.upto(number_of_entries) do |i|
  puts """@article{id#{i},
  title = {This is my title #{i}},
  author = {Firstname#{i} Lastname#{i} and FirstnameA#{i} LastnameA#{i} and FirstnameB#{i} LastnameB#{i}},
  journal = {Journal Title #{i}},
  year = {#{i}}
}
"""
end
