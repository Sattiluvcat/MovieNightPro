import sys
import requests
from bs4 import BeautifulSoup
import re
import json

def scrape_movie_info(url):
    upload_auth_token = "240508762:da3ebdbe1326cd7af1a75daffa0ba64f3a364996"
    header_movie = {
        'Authorization': f'Bearer {upload_auth_token}',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) '
                      'Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0',
        'Connection': 'keep-alive',
        'Referer': 'https://movie.douban.com/',
        'Cookie': 'll="118254"; bid=oOhP0ATcGuw; _pk_id.100001.8cb4=cc77c8016069698b.1751005721.; push_noty_num=0; '
                  'push_doumail_num=0; __yadk_uid=Dpf3cmENdo3XfHvyycgqF9rHJSb6Re0h; '
                  '_pk_ref.100001.8cb4=%5B%22%22%2C%22%22%2C1751428339%2C%22https%3A%2F%2Fwww.bing.com%2F%22%5D; '
                  '_pk_ses.100001.8cb4=1; dbcl2="240508762:xPRkUti2p/Q"; '
                  'ck=ccnh; frodotk_db="73e4cb32c21619f25bae619846424dd6"'
    }
    response_movie = requests.get(url, headers=header_movie)
    # response_movie.encoding = 'utf-8'  # Ensure the response encoding is set to UTF-8
    if response_movie.status_code != 200:
        return 'Error', 'Error', 'Error'
    soup_movie = BeautifulSoup(response_movie.text, 'html.parser')
    rating = soup_movie.select_one('div.rating_self.clearfix strong.ll.rating_num')
    rating = rating.get_text(strip=True) if rating else "暂无评分"
    photo_url = soup_movie.select_one('div.subject.clearfix #mainpic a.nbgnbg img').get('src')
    summary_element = soup_movie.select_one('span[property="v:summary"]')
    summary = "暂无简介"

    if summary_element:
        # 处理 <br> 标签 - 替换为空格
        for br in summary_element.find_all('br'):
            br.replace_with(' ')

        # 获取清理后的文本
        summary = summary_element.get_text(strip=True)

    return {
        "rating": rating,
        "cover": photo_url,
        "summary": summary
    }

if __name__ == '__main__':
    # url = 'https://movie.douban.com/subject/1295437/'
    url = sys.argv[1]
    movie_data = scrape_movie_info(url)

    # 输出JSON格式结果
    print(json.dumps(movie_data, ensure_ascii=False))