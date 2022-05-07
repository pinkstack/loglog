#!/usr/bin/env ruby
require 'uri'
require 'net/http'
require 'json'
require 'yaml'

def urls
  (0..20).map { |n| 'https://api.rtvslo.si/stats/logstream/live/%{n}' % { n: n } }
end

def fetch_stats(url)
  response = Net::HTTP.get_response(URI(url))
  JSON.parse(response.body, { symbolize_names: true }) if response.is_a?(Net::HTTPSuccess)
end

def read_data(response = {})
  { name: response.fetch(:response).fetch(:channel) }
end

channels = urls.map { |url| [url, read_data(fetch_stats(url))] }
               .select { |_, data| data[:name] }
               .map { |url, data| data.merge!(url: url, enabled: true) }
               .sort_by { |c| c[:name] }
               .map { |c| c.map { |k, v| [k.to_s, v.is_a?(String) ? v.to_s : v] }.to_h }

puts YAML.dump(channels)
