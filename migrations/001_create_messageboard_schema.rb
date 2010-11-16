class CreateSchema < ActiveRecord::Migration
  def self.up
    create_table :messageboard_messages do |t|
      t.integer :id, :limit => 11
      t.string :userid, :limit => 32
      t.timestamp :datetime
      t.string, :url, :limit => 4000
      t.text :text
    end

	add_index :messageboard_messages, :url, [:url, :userid]

    create_table :messageboard_user_preferences do |t|
      t.integer :id, :limit => 11	
      t.string :userid, :limit => 32
      t.string :messageboard_nick, :limit => 12
      t.string :language, :limit => 2
      t.boolean visibility
    end

    add_index :messageboard_user_preferences, :userid
  end

  def self.down
    remove_table :messageboard_messages
    remove_table :messageboard_user_preferences
  end
end
