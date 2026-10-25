package com.sumizuri.app.feature.browse.editor

// What a new source starts from: one that reads a library from a server of the person's own, and says in its comments what to change.
internal val ExtensionTemplate = """
// A source for Sumizuri. It is one file with a class named Source. This one reads your own library from a server you run, through a small JSON API. The globals are App for requests and settings, cheerio to read HTML, and CryptoJS. Change the address and the paths below to fit your server, then try each part on the Test tab. A source can reach public addresses only, so the server needs an address on the internet.

const SERVER = 'https://library.example.com';

// Asks the server for JSON.
async function get(path) {
  const manager = App.createRequestManager({ rateLimit: { requestsPerSecond: 4 } });
  const response = await manager.schedule(App.createRequest({ url: SERVER + path, method: 'GET' }));
  if (response.status < 200 || response.status >= 300) throw new Error('HTTP ' + response.status);
  return JSON.parse(response.data);
}

class Source {
  // The lists the source page shows as tabs. The first is opened first.
  getSourceFeeds() {
    return [
      { id: 'recent', name: 'Recently added' },
      { id: 'all', name: 'All titles' },
    ];
  }

  // The tags a search can use, as the server lists them.
  async getSearchTags() {
    const tags = await get('/api/tags');
    return tags.map((tag) => ({ id: tag.id, label: tag.name }));
  }

  // A search, or a feed when request.feed is set. Give back metadata for the next page, or nothing when the list ends.
  async getSearchResults(request, metadata) {
    const page = (metadata && metadata.page) || 1;
    const tags = (request.includedTags || []).map((tag) => '&tag=' + encodeURIComponent(tag.id)).join('');
    const query = '?page=' + page + '&sort=' + (request.feed || 'recent') + '&q=' + encodeURIComponent(request.title || '') + tags;
    const answer = await get('/api/titles' + query);
    const results = answer.items.map((item) => ({ mangaId: String(item.id), title: item.title, image: item.cover }));
    return { results: results, metadata: answer.hasMore ? { page: page + 1 } : undefined };
  }

  // What the page of a title shows.
  async getMangaDetails(mangaId) {
    const item = await get('/api/titles/' + mangaId);
    return {
      mangaInfo: {
        title: item.title,
        image: item.cover,
        author: item.author,
        desc: item.description,
        status: item.finished ? 'COMPLETED' : 'ONGOING',
        tags: item.tags || [],
      },
    };
  }

  // The chapters of a title, each with a number.
  async getChapters(mangaId) {
    const chapters = await get('/api/titles/' + mangaId + '/chapters');
    return chapters.map((chapter) => ({ chapterId: String(chapter.id), name: chapter.name, number: chapter.number, time: chapter.added }));
  }

  // The pictures of a chapter. A source of books gives its words as text instead.
  async getChapterDetails(mangaId, chapterId) {
    const chapter = await get('/api/chapters/' + chapterId);
    return { id: chapterId, mangaId: mangaId, pages: chapter.pages };
  }

  // A source of videos gives getVideos(mangaId, chapterId) instead of pages, returning a list of { url, quality }.
}

module.exports = { Source };
""".trimStart()
