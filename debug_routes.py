import urllib.request
import xml.etree.ElementTree as ET

url = "http://127.0.0.1:1200/rsshub/routes/zh"
print(f"Fetching {url} ...")
req = urllib.request.Request(url, headers={"Accept": "application/rss+xml"})
body = urllib.request.urlopen(req, timeout=30).read()

root = ET.fromstring(body)

# Show first 10 items: raw guid + title
items = root.findall(".//item")
print(f"\nTotal items: {len(items)}\n")

for i, item in enumerate(items[:15]):
    guid_el = item.find("guid")
    title_el = item.find("title")
    guid = guid_el.text if guid_el is not None else "MISSING"
    title = title_el.text if title_el is not None else "MISSING"
    print(f"[{i}] guid: {guid}")
    print(f"    title: {title}")
    print()

# Check for weibo
print("=" * 60)
print("Searching for weibo routes:")
for item in items:
    guid_el = item.find("guid")
    if guid_el is not None and guid_el.text and "weibo" in guid_el.text.lower():
        title_el = item.find("title")
        print(f"  guid: {guid_el.text}")
        print(f"  title: {title_el.text if title_el is not None else 'N/A'}")
        print()
