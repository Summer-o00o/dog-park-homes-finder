# Dog Park Homes Finder Demo Script

## 3-Minute Demo Script

### 0:00 - 0:25

Hi everyone, I'm the creator of Dog Park Homes Finder, and I built this for the Amazon Nova AI Hackathon.

A little background on why I made this.

We own a dog, and we recently relocated from Oklahoma to Washington. One real pain point during our move was finding a home that wasn't just in a good neighborhood, but also close to a great dog park. You can find home listings pretty easily, and you can search for parks separately, but it's surprisingly hard to answer one simple question:

Can I find a home near a dog park that my dog will actually love?

### 0:25 - 0:45

So I decided to build it.

Dog Park Homes Finder is a full-stack app that helps people search for homes for sale near highly rated dog parks. The user can enter a natural-language prompt, for example: "Seattle, under 900k, within walking distance of a clean dog park."

On screen:
- Show the home screen.
- Type the sample query slowly enough to read.

### 0:45 - 1:10

Instead of forcing users through rigid filters, the app lets them describe what they want naturally.

On screen:
- Click `Search`.
- Let the loading state appear briefly.

### 1:10 - 1:40

Amazon Nova powers the first key part of the experience.

I use Amazon Nova to parse the user's natural-language query into structured search filters like location, radius, and price range. That means the search experience feels much more natural, while the backend still gets the structured data it needs.

On screen:
- Show the results loading in.
- Keep the query visible for a moment.
- Scroll lightly into the listings.

### 1:40 - 2:10

Amazon Nova powers the second key part of the experience as well.

After I find dog parks through Google Places, I use Nova again to analyze real user reviews and turn them into structured signals like cleanliness, crowdedness, parking, dog-friendliness, and park size. This gives users a much more useful signal than just a star rating.

On screen:
- Focus on a listing and its nearest dog park information.
- Briefly show the map and the relationship between homes and dog parks.

### 2:10 - 2:30

Behind the scenes, the flow is straightforward.

The React frontend sends the query to my Spring Boot backend, Amazon Nova handles both query understanding and dog park review analysis, Google Places provides dog park data, and the real estate service returns nearby listings.

On screen:
- Briefly switch to the Mermaid architecture diagram in `README.md`.
- Trace the flow once from frontend to backend to external services.

### 2:30 - 3:00

What makes this project meaningful to me is that it came from a real problem we experienced as a family. Moving with a dog changes how you choose where to live. You're not just choosing a house, you're choosing a daily routine, a walking route, and a place where your dog can be happy too.

Amazon Nova helped me build a search experience that feels natural and intelligent instead of forcing users through a traditional filter-based workflow.

Thank you for watching, and I hope Dog Park Homes Finder helps more pet owners find homes their dogs will love.

On screen:
- Return to the app.
- End on the best combined view of listings and map.

## Suggested Demo Query

`Seattle, under 900k, within walking distance of a clean dog park`

## Recording Notes

- Use one query only.
- Keep the README diagram segment under 20 seconds.
- End the video on the product UI, not the diagram.
- If API latency is inconsistent, prepare a stable result state before recording.
