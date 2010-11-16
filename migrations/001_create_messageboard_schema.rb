class CreateSchema < ActiveRecord::Migration
  def self.up
    create_table :messageboard_messages do |t|
      t.integer :id, :limit => 11
      t.string :userid, :limit => 32
      t.timestamp :datetime
      t.url, :string, :limit => 4000
      t.text :text
    end

    add_index :id, :index

    create_table :messageboard_userPrefferences do |t|
      t.integer :id, :limit => 11	
      t.string :userid, :limit => 32
      t.string :messageboard_nick, :limit => 12
      t.string :language, :limit => 2
      t.boolean visibility
    end

    add_index :id
  end

  def self.down
    remove_table :messageboard_messages
    remove_table :messageboard_userPrefferences
  end
end
