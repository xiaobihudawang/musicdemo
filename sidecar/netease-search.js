const request = require('request');

const args = process.argv.slice(2);
const keywords = args[0] || '';
const curpage = parseInt(args[1]) || 1;

request.post({
    url: 'https://music.163.com/api/cloudsearch/pc',
    form: {
        s: keywords,
        offset: (curpage - 1) * 20,
        limit: 20,
        type: 1
    },
    headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        'Referer': 'https://music.163.com/',
        'Content-Type': 'application/x-www-form-urlencoded'
    },
    json: true
}, (err, resp, body) => {
    if (err) {
        console.log(JSON.stringify({ code: 500, message: err.message }));
        process.exit(1);
    }
    if (!body || body.code !== 200 || !body.result || !body.result.songs) {
        const msg = body ? JSON.stringify(body).substring(0, 300) : 'empty response';
        console.log(JSON.stringify({ code: 500, message: 'api error: ' + msg }));
        process.exit(1);
    }
    const songs = body.result.songs.map(s => ({
        id: 'netrack_' + s.id,
        title: s.name,
        artist: s.ar && s.ar[0] ? s.ar[0].name : 'unknown',
        artist_id: s.ar && s.ar[0] ? 'neartist_' + s.ar[0].id : '',
        album: s.al ? s.al.name : '',
        album_id: s.al ? 'nealbum_' + s.al.id : '',
        source: 'netease',
        source_url: 'http://music.163.com/#/song?id=' + s.id,
        img_url: s.al ? s.al.picUrl : '',
        url: 'netrack_' + s.id
    }));
    console.log(JSON.stringify({ code: 200, data: { result: songs, total: body.result.songCount } }));
    process.exit(0);
});
